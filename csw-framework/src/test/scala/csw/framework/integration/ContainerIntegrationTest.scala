package csw.framework.integration

import akka.actor.Status
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.Materializer.matFromSystem
import akka.stream.scaladsl.{Keep, Sink}
import akka.testkit
import com.typesafe.config.ConfigFactory
import csw.command.client.CommandServiceFactory
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.ComponentCommonMessage.{GetSupervisorLifecycleState, LifecycleStateSubscription}
import csw.command.client.messages.ContainerCommonMessage.{GetComponents, GetContainerLifecycleState}
import csw.command.client.messages.RunningMessage.Lifecycle
import csw.command.client.messages.SupervisorContainerCommonMessages.{Restart, Shutdown}
import csw.command.client.models.framework.PubSub.Subscribe
import csw.command.client.models.framework.ToComponentLifecycleMessage.{GoOffline, GoOnline}
import csw.command.client.models.framework.{Components, ContainerLifecycleState, LifecycleStateChanged, SupervisorLifecycleState}
import csw.common.FrameworkAssertions._
import csw.common.components.framework.SampleComponentState._
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.framework.internal.wiring.{Container, FrameworkWiring}
import csw.location.api.models
import csw.location.api.models.ComponentType.{Assembly, HCD}
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.api.models._
import csw.location.client.ActorSystemFactory
import csw.params.core.states.{CurrentState, StateName}
import csw.prefix.models.{Prefix, Subsystem}
import io.lettuce.core.RedisClient

import scala.concurrent.duration.DurationLong

// DEOPSCSW-169: Creation of Multiple Components
// DEOPSCSW-177: Hooks for lifecycle management
// DEOPSCSW-182: Control Life Cycle of Components
// DEOPSCSW-216: Locate and connect components to send AKKA commands
// CSW-82: ComponentInfo should take prefix
class ContainerIntegrationTest extends FrameworkIntegrationSuite {
  import testWiring.seedLocationService

  private val irisContainerConnection = AkkaConnection(
    ComponentId(Prefix(Subsystem.Container, "IRIS_Container"), ComponentType.Container)
  )
  private val filterAssembly: ComponentId = models.ComponentId(Prefix(Subsystem.TCS, "Filter"), Assembly)
  private val instrumentHcd: ComponentId  = models.ComponentId(Prefix(Subsystem.TCS, "Instrument_Filter"), HCD)
  private val disperserHcd: ComponentId   = models.ComponentId(Prefix(Subsystem.TCS, "Disperser"), HCD)

  private val filterAssemblyAkkaConnection = AkkaConnection(filterAssembly)
  private val filterAssemblyHttpConnection = HttpConnection(filterAssembly)
  private val instrumentHcdAkkaConnection  = AkkaConnection(instrumentHcd)
  private val instrumentHcdHttpConnection  = HttpConnection(instrumentHcd)
  private val disperserHcdAkkaConnection   = AkkaConnection(disperserHcd)
  private val disperserHcdHttpConnection   = HttpConnection(disperserHcd)

  private implicit val containerActorSystem: ActorSystem[SpawnProtocol.Command] =
    ActorSystemFactory.remote(SpawnProtocol(), "container-system")

  override def afterAll(): Unit = {
    containerActorSystem.terminate()
    containerActorSystem.whenTerminated.await
    super.afterAll()
  }

  private def assertConnectionIsRegistered[L <: Location](connection: TypedConnection[L]) = {
    val loc = seedLocationService.find(connection).await
    loc.isDefined shouldBe true
    loc.get
  }

  // DEOPSCSW-181: Multiple Examples for Lifecycle Support
  test(
    "should start multiple components within a single container and able to accept lifecycle messages | DEOPSCSW-182, DEOPSCSW-177, DEOPSCSW-181, DEOPSCSW-216, DEOPSCSW-169, DEOPSCSW-372"
  ) {

    val wiring = FrameworkWiring.make(containerActorSystem, mock[RedisClient])
    // start a container and verify it moves to running lifecycle state
    val containerRef =
      Container.spawn(ConfigFactory.load("container.conf"), wiring).await

    val componentsProbe              = TestProbe[Components]("comp-probe")
    val containerLifecycleStateProbe = TestProbe[ContainerLifecycleState]("container-lifecycle-state-probe")
    val assemblyProbe                = TestProbe[CurrentState]("assembly-state-probe")
    val filterProbe                  = TestProbe[CurrentState]("filter-state-probe")
    val disperserProbe               = TestProbe[CurrentState]("disperser-state-probe")

    val assemblyLifecycleStateProbe  = TestProbe[LifecycleStateChanged]("assembly-lifecycle-probe")
    val filterLifecycleStateProbe    = TestProbe[LifecycleStateChanged]("filter-lifecycle-probe")
    val disperserLifecycleStateProbe = TestProbe[LifecycleStateChanged]("disperser-lifecycle-probe")

    // initially container is put in Idle lifecycle state and wait for all the components to move into Running lifecycle state
    // ********** Message: GetContainerLifecycleState **********
    containerRef ! GetContainerLifecycleState(containerLifecycleStateProbe.ref)
    containerLifecycleStateProbe.expectMessage(ContainerLifecycleState.Idle)

    assertThatContainerIsRunning(containerRef, containerLifecycleStateProbe, 5.seconds)

    // resolve container using location service
    val containerLocation = seedLocationService.resolve(irisContainerConnection, 5.seconds).await

    containerLocation.isDefined shouldBe true
    val resolvedContainerRef = containerLocation.get.containerRef

    // ********** Message: GetComponents **********
    resolvedContainerRef ! GetComponents(componentsProbe.ref)
    val components = componentsProbe.expectMessageType[Components].components
    components.size shouldBe 3

    // To verify that Container gets in Running state once all components start
    // ********** Message: GetContainerLifecycleState **********
    containerRef ! GetContainerLifecycleState(containerLifecycleStateProbe.ref)
    containerLifecycleStateProbe.expectMessage(ContainerLifecycleState.Running)

    // resolve all the components from container using location service
    val filterAssemblyLocation = assertConnectionIsRegistered(filterAssemblyAkkaConnection)
    val instrumentHcdLocation  = assertConnectionIsRegistered(instrumentHcdAkkaConnection)
    val disperserHcdLocation   = assertConnectionIsRegistered(disperserHcdAkkaConnection)
    assertConnectionIsRegistered(filterAssemblyHttpConnection)
    assertConnectionIsRegistered(instrumentHcdHttpConnection)
    assertConnectionIsRegistered(disperserHcdHttpConnection)

    val assemblySupervisor  = filterAssemblyLocation.componentRef
    val filterSupervisor    = instrumentHcdLocation.componentRef
    val disperserSupervisor = disperserHcdLocation.componentRef

    val assemblyCommandService  = CommandServiceFactory.make(filterAssemblyLocation)
    val filterCommandService    = CommandServiceFactory.make(instrumentHcdLocation)
    val disperserCommandService = CommandServiceFactory.make(disperserHcdLocation)

    // DEOPSCSW-372: Provide an API for PubSubActor that hides actor based interaction
    // Subscribe to component's current state
    assemblyCommandService.subscribeCurrentState(assemblyProbe.ref ! _)
    filterCommandService.subscribeCurrentState(filterProbe.ref ! _)
    disperserCommandService.subscribeCurrentState(disperserProbe.ref ! _)

    // Subscribe to component's lifecycle state
    assemblySupervisor ! LifecycleStateSubscription(Subscribe(assemblyLifecycleStateProbe.ref))
    filterSupervisor ! LifecycleStateSubscription(Subscribe(filterLifecycleStateProbe.ref))
    disperserSupervisor ! LifecycleStateSubscription(Subscribe(disperserLifecycleStateProbe.ref))

    val supervisorLifecycleStateProbe = TestProbe[SupervisorLifecycleState]()
    assemblySupervisor ! GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref)
    filterSupervisor ! GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref)
    disperserSupervisor ! GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref)

    // make sure that all the components are in running lifecycle state before sending lifecycle messages
    supervisorLifecycleStateProbe.expectMessage(SupervisorLifecycleState.Running)
    supervisorLifecycleStateProbe.expectMessage(SupervisorLifecycleState.Running)
    supervisorLifecycleStateProbe.expectMessage(SupervisorLifecycleState.Running)

    // lifecycle messages gets forwarded to all components and their corresponding handlers gets invoked
    // ********** Message: Lifecycle(GoOffline) **********
    resolvedContainerRef ! Lifecycle(GoOffline)
    assemblyProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(offlineChoice))))
    filterProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(offlineChoice))))
    disperserProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(offlineChoice))))

    // Message: Lifecycle(GoOnline)
    resolvedContainerRef ! Lifecycle(GoOnline)
    assemblyProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(onlineChoice))))
    filterProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(onlineChoice))))
    disperserProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(onlineChoice))))

    // on Restart message, container falls back to Idle lifecycle state and wait for all the components to get restarted
    // and moves to Running lifecycle state
    // component handlers hooks should be invoked in following sequence:
    // 1. onShutdown (old TLA)
    // 2. initialize (new TLA)
    // 3. onRun (new TLA)
    // ********** Message: Restart **********
    resolvedContainerRef ! Restart

    resolvedContainerRef ! GetContainerLifecycleState(containerLifecycleStateProbe.ref)
    containerLifecycleStateProbe.expectMessage(ContainerLifecycleState.Idle)

    assemblyProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice))))
    filterProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice))))
    disperserProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice))))

    assemblyProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))
    filterProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))
    disperserProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))

    eventually {
      assemblyLifecycleStateProbe.expectMessage(
        LifecycleStateChanged(assemblySupervisor, SupervisorLifecycleState.Running)
      )
    }

    eventually {
      filterLifecycleStateProbe.expectMessage(
        LifecycleStateChanged(filterSupervisor, SupervisorLifecycleState.Running)
      )
    }

    eventually {
      disperserLifecycleStateProbe.expectMessage(
        LifecycleStateChanged(disperserSupervisor, SupervisorLifecycleState.Running)
      )
    }

    assertThatContainerIsRunning(resolvedContainerRef, containerLifecycleStateProbe, 2.seconds)

    // assert that all components are re-registered
    assertConnectionIsRegistered(filterAssemblyAkkaConnection)
    assertConnectionIsRegistered(instrumentHcdAkkaConnection)
    assertConnectionIsRegistered(disperserHcdAkkaConnection)
    assertConnectionIsRegistered(filterAssemblyHttpConnection)
    assertConnectionIsRegistered(instrumentHcdHttpConnection)
    assertConnectionIsRegistered(disperserHcdHttpConnection)

    // Using seedActorSystem in trackers as they are needed to be external to Container and components
    val containerTracker      = testkit.TestProbe()(testWiring.seedActorSystem.toClassic)
    val filterAssemblyTracker = testkit.TestProbe()(testWiring.seedActorSystem.toClassic)
    val instrumentHcdTracker  = testkit.TestProbe()(testWiring.seedActorSystem.toClassic)
    val disperserHcdTracker   = testkit.TestProbe()(testWiring.seedActorSystem.toClassic)

    // start tracking container and all the components, so that on Shutdown message, all the trackers gets LocationRemoved event
    seedLocationService
      .track(irisContainerConnection)
      .toMat(Sink.actorRef[TrackingEvent](containerTracker.ref, "Completed", t => Status.Failure(t)))(Keep.both)
      .run()(matFromSystem(testWiring.seedActorSystem))

    seedLocationService
      .track(filterAssemblyAkkaConnection)
      .toMat(Sink.actorRef[TrackingEvent](filterAssemblyTracker.ref, "Completed", t => Status.Failure(t)))(Keep.both)
      .run()(matFromSystem(testWiring.seedActorSystem))

    seedLocationService
      .track(instrumentHcdAkkaConnection)
      .toMat(Sink.actorRef[TrackingEvent](instrumentHcdTracker.ref, "Completed", t => Status.Failure(t)))(Keep.both)
      .run()(matFromSystem(testWiring.seedActorSystem))

    seedLocationService
      .track(disperserHcdAkkaConnection)
      .toMat(Sink.actorRef[TrackingEvent](disperserHcdTracker.ref, "Completed", t => Status.Failure(t)))(Keep.both)
      .run()(matFromSystem(testWiring.seedActorSystem))

    // ********** Message: Shutdown **********
    resolvedContainerRef ! Shutdown

    // this proves that ComponentBehaviors postStop signal gets invoked for all components
    // as onShutdownHook of all TLA gets invoked from postStop signal
    assemblyProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice))))
    filterProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice))))
    disperserProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice))))

    // this proves that postStop signal of all supervisor's gets invoked
    // as supervisor gets unregistered in postStop signal
    val filterAssemblyRemoved = filterAssemblyTracker.fishForSpecificMessage(5.seconds) { case x: LocationRemoved => x }
    val instrumentHcdRemoved  = instrumentHcdTracker.fishForSpecificMessage(5.seconds) { case x: LocationRemoved => x }
    val disperserHcdRemoved   = disperserHcdTracker.fishForSpecificMessage(5.seconds) { case x: LocationRemoved => x }
    val containerRemoved      = containerTracker.fishForSpecificMessage(5.seconds) { case x: LocationRemoved => x }

    filterAssemblyRemoved.connection shouldBe filterAssemblyAkkaConnection
    instrumentHcdRemoved.connection shouldBe instrumentHcdAkkaConnection
    disperserHcdRemoved.connection shouldBe disperserHcdAkkaConnection
    containerRemoved.connection shouldBe irisContainerConnection

    // this proves that on shutdown message, container's actor system gets terminated
    // if it does not get terminated in 5 seconds, future will fail which in turn fail this test
    containerActorSystem.whenTerminated.await
  }
}
