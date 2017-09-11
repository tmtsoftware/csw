package csw.common.framework.integration

import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.{ActorMaterializer, Materializer}
import akka.typed.{ActorRef, ActorSystem}
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.{actor, testkit}
import com.typesafe.config.ConfigFactory
import csw.common.components.SampleComponentState._
import csw.common.framework.internal.container.ContainerMode
import csw.common.framework.internal.supervisor.SupervisorMode
import csw.common.framework.internal.wiring.{Container, FrameworkWiring}
import csw.common.framework.models.ContainerCommonMessage.{GetComponents, GetContainerMode}
import csw.common.framework.models.PubSub.Subscribe
import csw.common.framework.models.RunningMessage.Lifecycle
import csw.common.framework.models.SupervisorCommonMessage.{ComponentStateSubscription, LifecycleStateSubscription}
import csw.common.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline}
import csw.common.framework.models._
import csw.param.states.CurrentState
import csw.services.location.commons.{BlockingUtils, ClusterSettings}
import csw.services.location.models.ComponentType.{Assembly, HCD}
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{ComponentId, ComponentType, LocationRemoved, TrackingEvent}
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationLong}

// DEOPSCSW-169: Creation of Multiple Components
// DEOPSCSW-182: Control Life Cycle of Components
// DEOPSCSW-216: Locate and connect components to send AKKA commands
class ContainerIntegrationTest extends FunSuite with Matchers with BeforeAndAfterAll {

  implicit val seedActorSystem: actor.ActorSystem     = ClusterSettings().onPort(3552).system
  private val containerActorSystem: actor.ActorSystem = ClusterSettings().joinLocal(3552).system

  implicit val typedSystem: ActorSystem[_]      = seedActorSystem.toTyped
  implicit val testKitSettings: TestKitSettings = TestKitSettings(typedSystem)

  implicit val mat: Materializer               = ActorMaterializer()
  private val locationService: LocationService = LocationServiceFactory.withSystem(seedActorSystem)

  private val irisContainerConnection  = AkkaConnection(ComponentId("IRIS_Container", ComponentType.Container))
  private val filterAssemblyConnection = AkkaConnection(ComponentId("Filter", Assembly))
  private val instrumentHcdConnection  = AkkaConnection(ComponentId("Instrument_Filter", HCD))
  private val disperserHcdConnection   = AkkaConnection(ComponentId("Disperser", HCD))

  override protected def afterAll(): Unit = Await.result(seedActorSystem.terminate(), 5.seconds)

  def waitForContainerToMoveIntoRunningMode(
      containerRef: ActorRef[ContainerExternalMessage],
      probe: TestProbe[ContainerMode],
      duration: Duration
  ): Boolean = {

    def getContainerMode: ContainerMode = {
      containerRef ! GetContainerMode(probe.ref)
      probe.expectMsgType[ContainerMode]
    }

    BlockingUtils.poll(getContainerMode == ContainerMode.Running, duration)
  }

  test("should start multiple components withing a single container and able to accept lifecycle messages") {

    val wiring = FrameworkWiring.make(containerActorSystem)
    // start a container and verify it moves to running mode
    val containerRef = Container.spawn(ConfigFactory.load("container.conf"), wiring)

    val componentsProbe    = TestProbe[Components]("comp-probe")
    val containerModeProbe = TestProbe[ContainerMode]("container-mode-probe")
    val assemblyProbe      = TestProbe[CurrentState]("assembly-state-probe")
    val filterProbe        = TestProbe[CurrentState]("filter-state-probe")
    val disperserProbe     = TestProbe[CurrentState]("disperser-state-probe")

    val assemblyLifecycleStateProbe  = TestProbe[LifecycleStateChanged]("assembly-lifecycle-probe")
    val filterLifecycleStateProbe    = TestProbe[LifecycleStateChanged]("filter-lifecycle-probe")
    val disperserLifecycleStateProbe = TestProbe[LifecycleStateChanged]("disperser-lifecycle-probe")

    // initially container is put in Idle state and wait for all the components to move into Running mode
    // ********** Message: GetContainerMode **********
    containerRef ! GetContainerMode(containerModeProbe.ref)
    containerModeProbe.expectMsg(ContainerMode.Idle)

    // resolve container using location service
    val containerLocation = Await.result(locationService.resolve(irisContainerConnection, 5.seconds), 5.seconds)

    containerLocation.isDefined shouldBe true
    val resolvedContainerRef = containerLocation.get.typedRef[ContainerExternalMessage]

    // ********** Message: GetComponents **********
    resolvedContainerRef ! GetComponents(componentsProbe.ref)
    val components = componentsProbe.expectMsgType[Components].components
    components.size shouldBe 3

    waitForContainerToMoveIntoRunningMode(containerRef, containerModeProbe, 5.seconds)

    // resolve all the components from container using location service
    val filterAssemblyLocation = Await.result(locationService.find(filterAssemblyConnection), 5.seconds)
    val instrumentHcdLocation  = Await.result(locationService.find(instrumentHcdConnection), 5.seconds)
    val disperserHcdLocation   = Await.result(locationService.find(disperserHcdConnection), 5.seconds)

    filterAssemblyLocation.isDefined shouldBe true
    instrumentHcdLocation.isDefined shouldBe true
    disperserHcdLocation.isDefined shouldBe true

    val assemblySupervisor  = filterAssemblyLocation.get.typedRef[SupervisorExternalMessage]
    val filterSupervisor    = instrumentHcdLocation.get.typedRef[SupervisorExternalMessage]
    val disperserSupervisor = disperserHcdLocation.get.typedRef[SupervisorExternalMessage]

    // once all components from container moves to Running mode,
    // container moves to Running mode and ready to accept external lifecycle messages
    resolvedContainerRef ! GetContainerMode(containerModeProbe.ref)
    containerModeProbe.expectMsg(ContainerMode.Running)

    // Subscribe to component's current state
    assemblySupervisor ! ComponentStateSubscription(Subscribe(assemblyProbe.ref))
    filterSupervisor ! ComponentStateSubscription(Subscribe(filterProbe.ref))
    disperserSupervisor ! ComponentStateSubscription(Subscribe(disperserProbe.ref))

    // Subscribe to component's lifecycle state
    assemblySupervisor ! LifecycleStateSubscription(Subscribe(assemblyLifecycleStateProbe.ref))
    filterSupervisor ! LifecycleStateSubscription(Subscribe(filterLifecycleStateProbe.ref))
    disperserSupervisor ! LifecycleStateSubscription(Subscribe(disperserLifecycleStateProbe.ref))

    Thread.sleep(50)
    // lifecycle messages gets forwarded to all components and their corresponding handlers gets invoked
    // ********** Message: Lifecycle(GoOffline) **********
    resolvedContainerRef ! Lifecycle(GoOffline)
    assemblyProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(offlineChoice))))
    filterProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(offlineChoice))))
    disperserProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(offlineChoice))))

    // Message: Lifecycle(GoOnline)
    resolvedContainerRef ! Lifecycle(GoOnline)
    assemblyProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(onlineChoice))))
    filterProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(onlineChoice))))
    disperserProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(onlineChoice))))

    // on Restart message, container falls back to Idle mode and wait for all the components to get restarted
    // and moves to Running mode
    // component handlers hooks should be invoked in following sequence:
    // 1. onShutdown (old TLA)
    // 2. initialize (new TLA)
    // 3. onRun (new TLA)
    // ********** Message: Restart **********
    resolvedContainerRef ! Restart

    resolvedContainerRef ! GetContainerMode(containerModeProbe.ref)
    containerModeProbe.expectMsg(ContainerMode.Idle)

    assemblyProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(shutdownChoice))))
    filterProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(shutdownChoice))))
    disperserProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(shutdownChoice))))

    assemblyProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(initChoice))))
    filterProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(initChoice))))
    disperserProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(initChoice))))

    assemblyLifecycleStateProbe.expectMsg(LifecycleStateChanged(assemblySupervisor, SupervisorMode.Running))
    filterLifecycleStateProbe.expectMsg(LifecycleStateChanged(filterSupervisor, SupervisorMode.Running))
    disperserLifecycleStateProbe.expectMsg(LifecycleStateChanged(disperserSupervisor, SupervisorMode.Running))

    Thread.sleep(100)
    resolvedContainerRef ! GetContainerMode(containerModeProbe.ref)
    containerModeProbe.expectMsg(ContainerMode.Running)

    val containerTracker      = testkit.TestProbe()
    val filterAssemblyTracker = testkit.TestProbe()
    val instrumentHcdTracker  = testkit.TestProbe()
    val disperserHcdTracker   = testkit.TestProbe()

    // start tracking container and all the components, so that on Shutdown message, all the trackers gets LocationRemoved event
    locationService
      .track(irisContainerConnection)
      .toMat(Sink.actorRef[TrackingEvent](containerTracker.ref, "Completed"))(Keep.both)
      .run()

    locationService
      .track(filterAssemblyConnection)
      .toMat(Sink.actorRef[TrackingEvent](filterAssemblyTracker.ref, "Completed"))(Keep.both)
      .run()

    locationService
      .track(instrumentHcdConnection)
      .toMat(Sink.actorRef[TrackingEvent](instrumentHcdTracker.ref, "Completed"))(Keep.both)
      .run()

    locationService
      .track(disperserHcdConnection)
      .toMat(Sink.actorRef[TrackingEvent](disperserHcdTracker.ref, "Completed"))(Keep.both)
      .run()

    // ********** Message: Shutdown **********

    resolvedContainerRef ! Shutdown

    assemblyProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(shutdownChoice))))
    filterProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(shutdownChoice))))
    disperserProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(shutdownChoice))))

    val filterAssemblyRemoved = filterAssemblyTracker.fishForSpecificMessage(5.seconds) { case x: LocationRemoved ⇒ x }
    val instrumentHcdRemoved  = instrumentHcdTracker.fishForSpecificMessage(5.seconds) { case x: LocationRemoved  ⇒ x }
    val disperserHcdRemoved   = disperserHcdTracker.fishForSpecificMessage(5.seconds) { case x: LocationRemoved   ⇒ x }
    val containerRemoved      = containerTracker.fishForSpecificMessage(5.seconds) { case x: LocationRemoved      ⇒ x }

    filterAssemblyRemoved.connection shouldBe filterAssemblyConnection
    instrumentHcdRemoved.connection shouldBe instrumentHcdConnection
    disperserHcdRemoved.connection shouldBe disperserHcdConnection
    containerRemoved.connection shouldBe irisContainerConnection

  }

}
