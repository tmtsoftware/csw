package csw.framework.integration

import akka.actor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.typed.TestKitSettings
import akka.testkit.typed.scaladsl.TestProbe
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.common.FrameworkAssertions._
import csw.common.components.framework.SampleComponentState._
import csw.framework.internal.wiring.{Container, FrameworkWiring, Standalone}
import csw.messages.scaladsl.SupervisorContainerCommonMessages.Shutdown
import csw.messages.commands
import csw.messages.commands.CommandName
import csw.messages.framework.{ContainerLifecycleState, SupervisorLifecycleState}
import csw.messages.location.ComponentId
import csw.messages.location.ComponentType.{Assembly, HCD}
import csw.messages.location.Connection.AkkaConnection
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.messages.params.states.{CurrentState, StateName}
import csw.services.command.scaladsl.CommandService
import csw.services.location.commons.ClusterSettings
import csw.services.location.models.{HttpRegistration, TcpRegistration}
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}
import csw.services.logging.commons.LogAdminActorFactory
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, TimeoutException}

class TrackConnectionsIntegrationTest extends FunSuite with Matchers with BeforeAndAfterAll {

  implicit val seedActorSystem: actor.ActorSystem = ClusterSettings().onPort(3554).system

  implicit val typedSystem: ActorSystem[_]      = seedActorSystem.toTyped
  implicit val testKitSettings: TestKitSettings = TestKitSettings(typedSystem)

  implicit val mat: Materializer               = ActorMaterializer()
  private val locationService: LocationService = LocationServiceFactory.withSystem(seedActorSystem)

  private val filterAssemblyConnection = AkkaConnection(ComponentId("Filter", Assembly))
  private val disperserHcdConnection   = AkkaConnection(ComponentId("Disperser", HCD))

  override protected def afterAll(): Unit = {
    Await.result(seedActorSystem.terminate(), 5.seconds)

  }

  // DEOPSCSW-218: Discover component connection information using Akka protocol
  // DEOPSCSW-220: Access and Monitor components for current values
  // DEOPSCSW-221: Avoid sending commands to non-executing components
  test("should track connections when locationServiceUsage is RegisterAndTrackServices") {
    val containerActorSystem: actor.ActorSystem = ClusterSettings().joinLocal(3554).system
    val wiring: FrameworkWiring                 = FrameworkWiring.make(containerActorSystem)
    // start a container and verify it moves to running lifecycle state
    val containerRef =
      Await.result(Container.spawn(ConfigFactory.load("container_tracking_connections.conf"), wiring), 5.seconds)

    val containerLifecycleStateProbe = TestProbe[ContainerLifecycleState]("container-lifecycle-state-probe")
    val assemblyProbe                = TestProbe[CurrentState]("assembly-state-probe")

    // initially container is put in Idle lifecycle state and wait for all the components to move into Running lifecycle state
    // ********** Message: GetContainerLifecycleState **********
    assertThatContainerIsRunning(containerRef, containerLifecycleStateProbe, 5.seconds)

    // resolve all the components from container using location service
    val filterAssemblyLocation = Await.result(locationService.find(filterAssemblyConnection), 5.seconds)
    val disperserHcdLocation   = Await.result(locationService.find(disperserHcdConnection), 5.seconds)

    val assemblyCommandService = new CommandService(filterAssemblyLocation.get)

    val disperserComponentRef   = disperserHcdLocation.get.componentRef
    val disperserCommandService = new CommandService(disperserHcdLocation.get)

    // Subscribe to component's current state
    assemblyCommandService.subscribeCurrentState(assemblyProbe.ref ! _)

    // assembly is tracking two HCD's, hence assemblyProbe will receive LocationUpdated event from two HCD's
    assemblyProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(akkaLocationUpdatedChoice))))
    assemblyProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(akkaLocationUpdatedChoice))))

    // if one of the HCD shuts down, then assembly should know and receive LocationRemoved event
    disperserComponentRef ! Shutdown
    assemblyProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(akkaLocationRemovedChoice))))

    implicit val timeout: Timeout = Timeout(100.millis)
    intercept[TimeoutException] {
      Await.result(disperserCommandService.submit(commands.Setup(prefix, CommandName("isAlive"), None)), 200.millis)
    }

    Await.result(wiring.locationService.shutdown(TestFinishedReason), 5.seconds)
  }

  /**
   * If component writer wants to track additional connections which are not specified in configuration file,
   * then using trackConnection(connection: Connection) hook which is added in ComponentHandlers can be used
   * Uses of this are shown in [[csw.common.components.framework.SampleComponentHandlers]]
   * */
  //DEOPSCSW-219 Discover component connection using HTTP protocol
  test("component should be able to track http and tcp connections") {
    val actorSystem: actor.ActorSystem = ClusterSettings().joinLocal(3554).system
    val wiring: FrameworkWiring        = FrameworkWiring.make(actorSystem)
    // start component in standalone mode
    Standalone.spawn(ConfigFactory.load("standalone.conf"), wiring)

    val supervisorLifecycleStateProbe = TestProbe[SupervisorLifecycleState]("supervisor-lifecycle-state-probe")
    val akkaConnection                = AkkaConnection(ComponentId("IFS_Detector", HCD))

    // verify component gets registered with location service
    val eventualLocation = locationService.resolve(akkaConnection, 5.seconds)
    val maybeLocation    = Await.result(eventualLocation, 5.seconds)

    maybeLocation.isDefined shouldBe true
    val resolvedAkkaLocation = maybeLocation.get
    resolvedAkkaLocation.connection shouldBe akkaConnection

    val assemblySupervisor = resolvedAkkaLocation.componentRef
    assertThatSupervisorIsRunning(assemblySupervisor, supervisorLifecycleStateProbe, 5.seconds)

    val assemblyProbe          = TestProbe[CurrentState]("assembly-state-probe")
    val assemblyCommandService = new CommandService(resolvedAkkaLocation)
    // Subscribe to component's current state
    assemblyCommandService.subscribeCurrentState(assemblyProbe.ref ! _)

    // register http connection
    Await.result(
      locationService.register(
        HttpRegistration(httpConnection, 9090, "test/path", LogAdminActorFactory.make(actorSystem))
      ),
      5.seconds
    )

    // assembly is tracking HttpConnection that we registered above, hence assemblyProbe will receive LocationUpdated event
    assemblyProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(httpLocationUpdatedChoice))))

    // On unavailability of HttpConnection, the assembly should know and receive LocationRemoved event
    locationService.unregister(httpConnection)
    assemblyProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(httpLocationRemovedChoice))))

    // register tcp connection
    Await.result(
      locationService.register(TcpRegistration(tcpConnection, 9090, LogAdminActorFactory.make(actorSystem))),
      5.seconds
    )

    // assembly is tracking TcpConnection that we registered above, hence assemblyProbe will receive LocationUpdated event.
    assemblyProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(tcpLocationUpdatedChoice))))

    // On unavailability of TcpConnection, the assembly should know and receive LocationRemoved event
    locationService.unregister(tcpConnection)
    assemblyProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(tcpLocationRemovedChoice))))

    Await.result(wiring.locationService.shutdown(TestFinishedReason), 5.seconds)
  }

}
