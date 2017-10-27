package csw.framework.integration

import akka.actor
import akka.stream.{ActorMaterializer, Materializer}
import akka.typed.ActorSystem
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import com.typesafe.config.ConfigFactory
import csw.common.FrameworkAssertions._
import csw.common.components.SampleComponentState._
import csw.framework.internal.wiring.{Container, FrameworkWiring, Standalone}
import csw.messages.PubSub.Subscribe
import csw.messages.Shutdown
import csw.messages.SupervisorCommonMessage.ComponentStateSubscription
import csw.messages.framework.{ContainerLifecycleState, SupervisorLifecycleState}
import csw.messages.location.ComponentId
import csw.messages.location.ComponentType.{Assembly, HCD}
import csw.messages.location.Connection.AkkaConnection
import csw.messages.params.states.CurrentState
import csw.services.location.commons.ClusterSettings
import csw.services.location.models.{HttpRegistration, TcpRegistration}
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}
import csw.services.logging.scaladsl.LogAdminActorFactory
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

// DEOPSCSW-218: Discover component connection information using Akka protocol
// DEOPSCSW-220: Access and Monitor components for current values
// DEOPSCSW-221: Avoid sending commands to non-executing components
class TrackConnectionsIntegrationTest extends FunSuite with Matchers with BeforeAndAfterAll {

  implicit val seedActorSystem: actor.ActorSystem = ClusterSettings().onPort(3552).system

  implicit val typedSystem: ActorSystem[_]      = seedActorSystem.toTyped
  implicit val testKitSettings: TestKitSettings = TestKitSettings(typedSystem)

  implicit val mat: Materializer               = ActorMaterializer()
  private val locationService: LocationService = LocationServiceFactory.withSystem(seedActorSystem)

  private val filterAssemblyConnection = AkkaConnection(ComponentId("Filter", Assembly))
  private val disperserHcdConnection   = AkkaConnection(ComponentId("Disperser", HCD))

  override protected def afterAll(): Unit = {
    Await.result(seedActorSystem.terminate(), 5.seconds)

  }

  test("should track connections when locationServiceUsage is RegisterAndTrackServices") {
    val containerActorSystem: actor.ActorSystem = ClusterSettings().joinLocal(3552).system
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

    val assemblySupervisor  = filterAssemblyLocation.get.componentRef()
    val disperserSupervisor = disperserHcdLocation.get.componentRef()

    // Subscribe to component's current state
    assemblySupervisor ! ComponentStateSubscription(Subscribe(assemblyProbe.ref))

    // assembly is tracking two HCD's, hence assemblyProbe will receive LocationUpdated event from two HCD's
    assemblyProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(akkaLocationUpdatedChoice))))
    assemblyProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(akkaLocationUpdatedChoice))))

    // if one of the HCD shuts down, then assembly should know and receive LocationRemoved event
    disperserSupervisor ! Shutdown
    assemblyProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(akkaLocationRemovedChoice))))
    Await.result(wiring.locationService.shutdown(), 5.seconds)
  }

  test("component should be able to track http and tcp connections") {
    val actorSystem: actor.ActorSystem = ClusterSettings().joinLocal(3552).system
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

    val assemblySupervisor = resolvedAkkaLocation.componentRef()
    assertThatSupervisorIsRunning(assemblySupervisor, supervisorLifecycleStateProbe, 5.seconds)

    val assemblyProbe = TestProbe[CurrentState]("assembly-state-probe")

    // Subscribe to component's current state
    assemblySupervisor ! ComponentStateSubscription(Subscribe(assemblyProbe.ref))
    Await.result(wiring.locationService.register(
                   HttpRegistration(httpConnection, 9090, "test/path", LogAdminActorFactory.make(actorSystem))
                 ),
                 5.seconds)

    // assembly is tracking two HCD's, hence assemblyProbe will receive LocationUpdated event from two HCD's
    assemblyProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(httpLocationUpdatedChoice))))

    // if one of the HCD shuts down, then assembly should know and receive LocationRemoved event
    wiring.locationService.unregister(httpConnection)
    assemblyProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(httpLocationRemovedChoice))))

    Await.result(
      wiring.locationService.register(TcpRegistration(tcpConnection, 9090, LogAdminActorFactory.make(actorSystem))),
      5.seconds
    )

    // assembly is tracking two HCD's, hence assemblyProbe will receive LocationUpdated event from two HCD's
    assemblyProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(tcpLocationUpdatedChoice))))

    // if one of the HCD shuts down, then assembly should know and receive LocationRemoved event
    wiring.locationService.unregister(tcpConnection)
    assemblyProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(tcpLocationRemovedChoice))))

    Await.result(wiring.locationService.shutdown(), 5.seconds)
  }

}
