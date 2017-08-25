package csw.common.framework.scaladsl.integration

import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, Behavior}
import csw.common.ccs.CommandStatus.CommandResponse
import csw.common.ccs.DemandMatcher
import csw.common.components.ComponentStatistics
import csw.common.framework.FrameworkComponentTestInfos._
import csw.common.framework.FrameworkComponentTestSuite
import csw.common.framework.internal.SupervisorMode
import csw.common.framework.models.CommandMsg.Oneway
import csw.common.framework.models.SupervisorCommonMsg.{ComponentStateSubscription, LifecycleStateSubscription}
import csw.common.framework.models.PubSub.Subscribe
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline, Restart}
import csw.common.framework.models.{LifecycleStateChanged, SupervisorExternalMessage, ToComponentLifecycleMessage}
import csw.common.framework.scaladsl.SupervisorBehaviorFactory
import csw.param.commands.{CommandInfo, Setup}
import csw.param.generics.{KeyType, Parameter}
import csw.param.states.{CurrentState, DemandState}
import csw.services.location.scaladsl.LocationService
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

// DEOPSCSW-163: Provide admin facilities in the framework through Supervisor role
// DEOPSCSW-165: CSW Assembly Creation
// DEOPSCSW-166: CSW HCD Creation
// DEOPSCSW-176: Provide Infrastructure to manage TMT lifecycle
// DEOPSCSW-177: Hooks for lifecycle management
class FrameworkIntegrationTest extends FrameworkComponentTestSuite with MockitoSugar with BeforeAndAfterEach {
  import csw.common.components.SampleComponentState._

  var compStateProbe: TestProbe[CurrentState]                 = _
  var lifecycleStateProbe: TestProbe[LifecycleStateChanged]   = _
  var supervisorBehavior: Behavior[SupervisorExternalMessage] = _
  var supervisorRef: ActorRef[SupervisorExternalMessage]      = _
  val locationService                                         = mock[LocationService]

  def createSupervisorAndStartTLA(): Unit = {
    compStateProbe = TestProbe[CurrentState]
    lifecycleStateProbe = TestProbe[LifecycleStateChanged]
    supervisorBehavior = SupervisorBehaviorFactory.behavior(hcdInfo, locationService)
    // it creates supervisor which in turn spawns components TLA and sends Initialize and Run message to TLA
    supervisorRef = Await.result(system.systemActorOf(supervisorBehavior, "comp-supervisor"), 5.seconds)
    Thread.sleep(200)
    supervisorRef ! ComponentStateSubscription(Subscribe(compStateProbe.ref))
    supervisorRef ! LifecycleStateSubscription(Subscribe(lifecycleStateProbe.ref))
  }

  test("onInitialized and onRun hooks of comp handlers should be invoked when supervisor creates comp") {
    compStateProbe = TestProbe[CurrentState]
    lifecycleStateProbe = TestProbe[LifecycleStateChanged]
    supervisorBehavior = SupervisorBehaviorFactory.behavior(hcdInfo, locationService)

    // it creates supervisor which in turn spawns components TLA and sends Initialize and Run message to TLA
    supervisorRef = Await.result(system.systemActorOf(supervisorBehavior, "hcd-supervisor"), 5.seconds)
    supervisorRef ! ComponentStateSubscription(Subscribe(compStateProbe.ref))
    supervisorRef ! LifecycleStateSubscription(Subscribe(lifecycleStateProbe.ref))

    val initCurrentState = compStateProbe.expectMsgType[CurrentState]
    val initDemandState  = DemandState(prefix, Set(choiceKey.set(initChoice)))
    DemandMatcher(initDemandState).check(initCurrentState) shouldBe true

    val runCurrentState = compStateProbe.expectMsgType[CurrentState]
    val runDemandState  = DemandState(prefix, Set(choiceKey.set(runChoice)))
    DemandMatcher(runDemandState).check(runCurrentState) shouldBe true

    lifecycleStateProbe.expectMsg(LifecycleStateChanged(SupervisorMode.Running, supervisorRef))
  }

  // DEOPSCSW-179: Unique Action for a component
  test("onDomainMsg hook of comp handlers should be invoked when supervisor receives Domain message") {
    createSupervisorAndStartTLA()

    supervisorRef ! ComponentStatistics(1)

    val domainCurrentState = compStateProbe.expectMsgType[CurrentState]
    val domainDemandState  = DemandState(prefix, Set(choiceKey.set(domainChoice)))
    DemandMatcher(domainDemandState).check(domainCurrentState) shouldBe true
  }

  test("onControlCommand hook of comp handlers should be invoked when supervisor receives Control command") {
    createSupervisorAndStartTLA()

    val commandInfo: CommandInfo = "Obs001"
    val param: Parameter[Int]    = KeyType.IntKey.make("encoder").set(22)
    val setup: Setup             = Setup(commandInfo, prefix, Set(param))

    supervisorRef ! Oneway(setup, TestProbe[CommandResponse].ref)

    val commandCurrentState = compStateProbe.expectMsgType[CurrentState]
    val commandDemandState  = DemandState(prefix, Set(choiceKey.set(commandChoice)))
    DemandMatcher(commandDemandState).check(commandCurrentState) shouldBe true
  }

  test("onGoOffline and goOnline hooks of comp handlers should be invoked when supervisor receives Lifecycle messages") {
    createSupervisorAndStartTLA()

    supervisorRef ! Lifecycle(GoOffline)

    val offlineCurrentState = compStateProbe.expectMsgType[CurrentState]
    val offlineDemandState  = DemandState(prefix, Set(choiceKey.set(offlineChoice)))
    DemandMatcher(offlineDemandState).check(offlineCurrentState) shouldBe true

    supervisorRef ! Lifecycle(GoOnline)

    val onlineCurrentState = compStateProbe.expectMsgType[CurrentState]
    val onlineDemandState  = DemandState(prefix, Set(choiceKey.set(onlineChoice)))
    DemandMatcher(onlineDemandState).check(onlineCurrentState) shouldBe true
  }

  test("running component should ignore RunOnline lifecycle message when it is already online") {
    createSupervisorAndStartTLA()

    supervisorRef ! Lifecycle(GoOnline)
    compStateProbe.expectNoMsg(1.seconds)
  }

  test("running component should ignore RunOffline lifecycle message when it is already offline") {
    createSupervisorAndStartTLA()

    supervisorRef ! Lifecycle(GoOffline)
    compStateProbe.expectMsgType[CurrentState]

    supervisorRef ! Lifecycle(GoOffline)
    compStateProbe.expectNoMsg(1.seconds)

    supervisorRef ! Lifecycle(GoOnline)
    compStateProbe.expectMsgType[CurrentState]
  }

  test("onRestart hook of comp handlers should be invoked when supervisor receives Restart message") {
    createSupervisorAndStartTLA()

    supervisorRef ! Lifecycle(Restart)

    val restartCurrentState = compStateProbe.expectMsgType[CurrentState]
    val restartDemandState  = DemandState(prefix, Set(choiceKey.set(restartChoice)))
    DemandMatcher(restartDemandState).check(restartCurrentState) shouldBe true

    val initCurrentState = compStateProbe.expectMsgType[CurrentState]
    val initDemandState  = DemandState(prefix, Set(choiceKey.set(initChoice)))
    DemandMatcher(initDemandState).check(initCurrentState) shouldBe true

    val runCurrentState = compStateProbe.expectMsgType[CurrentState]
    val runDemandState  = DemandState(prefix, Set(choiceKey.set(runChoice)))
    DemandMatcher(runDemandState).check(runCurrentState) shouldBe true
  }

  test("onShutdown hook of comp handlers should be invoked when supervisor receives Shutdown message") {
    createSupervisorAndStartTLA()

    supervisorRef ! Lifecycle(ToComponentLifecycleMessage.Shutdown)

    val shutdownCurrentState = compStateProbe.expectMsgType[CurrentState]
    val shutdownDemandState  = DemandState(prefix, Set(choiceKey.set(shutdownChoice)))
    DemandMatcher(shutdownDemandState).check(shutdownCurrentState) shouldBe true

    lifecycleStateProbe.expectMsg(LifecycleStateChanged(SupervisorMode.PreparingToShutdown, supervisorRef))
  }

  // DEOPSCSW-179: Unique Action for a component
  test("supervisor should receive ShutdownFailure message when component fails to shutdown") {
    val stateChangedProbe = TestProbe[LifecycleStateChanged]
    supervisorBehavior = SupervisorBehaviorFactory.behavior(assemblyInfoToSimulateFailure, locationService)
    // it creates supervisor which in turn spawns components TLA and sends Initialize and Run message to TLA
    supervisorRef = Await.result(system.systemActorOf(supervisorBehavior, "comp-supervisor"), 5.seconds)
    Thread.sleep(200)
    // Registering probe to receive Lifecycle state changed events
    supervisorRef ! LifecycleStateSubscription(Subscribe(stateChangedProbe.ref))

    supervisorRef ! Lifecycle(ToComponentLifecycleMessage.Shutdown)

    // On receiving Shutdown message, Supervisor publishes its state -> PreparingToShutdown
    stateChangedProbe.expectMsg(LifecycleStateChanged(SupervisorMode.PreparingToShutdown, supervisorRef))
    // On shutdown failure, Supervisor publishes its state -> ShutdownFailure
    stateChangedProbe.expectMsg(LifecycleStateChanged(SupervisorMode.ShutdownFailure, supervisorRef))
  }

}
