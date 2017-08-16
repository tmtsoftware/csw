package csw.common.framework.scaladsl.integration

import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, Behavior}
import csw.common.ccs.CommandStatus.CommandResponse
import csw.common.ccs.DemandMatcher
import csw.common.components.hcd.SampleHcdAxisStatistics
import csw.common.framework.models.CommandMsg.Oneway
import csw.common.framework.models.CommonSupervisorMsg.ComponentStateSubscription
import csw.common.framework.models.PubSub.Subscribe
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline, Restart}
import csw.common.framework.models.{SupervisorMsg, ToComponentLifecycleMessage}
import csw.common.framework.scaladsl.{FrameworkComponentTestSuite, SupervisorBehaviorFactory}
import csw.param.commands.{CommandInfo, Setup}
import csw.param.generics.{KeyType, Parameter}
import csw.param.states.{CurrentState, DemandState}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

// DEOPSCSW-166: CSW HCD Creation
class HcdIntegrationTest extends FrameworkComponentTestSuite with MockitoSugar with BeforeAndAfterEach {
  import csw.common.components.hcd.SampleHcdHandlers._

  val compStateProbe: TestProbe[CurrentState]     = TestProbe[CurrentState]
  val supervisorBehavior: Behavior[SupervisorMsg] = SupervisorBehaviorFactory.make(hcdInfo)
  var supervisorRef: ActorRef[SupervisorMsg]      = _

  override protected def beforeEach(): Unit = {
    supervisorRef = Await.result(system.systemActorOf(supervisorBehavior, "hcd-supervisor"), 5.seconds)
    supervisorRef ! ComponentStateSubscription(Subscribe(compStateProbe.ref))
  }

  def consumeMsgsTillRunningState(): Unit = {
    compStateProbe.expectMsgType[CurrentState]
    compStateProbe.expectMsgType[CurrentState]
  }

  test("onInitialized and onRun hooks of hcd comp handlers should be invoked when supervisor creates hcd comp") {

    val initCurrentState = compStateProbe.expectMsgType[CurrentState]
    val initDemandState  = DemandState(prefix, Set(choiceKey.set(initChoice)))
    DemandMatcher(initDemandState).check(initCurrentState) shouldBe true

    val runCurrentState = compStateProbe.expectMsgType[CurrentState]
    val runDemandState  = DemandState(prefix, Set(choiceKey.set(runChoice)))
    DemandMatcher(runDemandState).check(runCurrentState) shouldBe true
  }

  test("onDomainMsg hook of hcd comp handlers should be invoked when supervisor receives Domain message") {

    consumeMsgsTillRunningState()

    supervisorRef ! SampleHcdAxisStatistics(1)

    val domainCurrentState = compStateProbe.expectMsgType[CurrentState]
    val domainDemandState  = DemandState(prefix, Set(choiceKey.set(domainChoice)))
    DemandMatcher(domainDemandState).check(domainCurrentState) shouldBe true
  }

  test("onControlCommand hook of hcd comp handlers should be invoked when supervisor receives Control command") {
    val commandInfo: CommandInfo = "Obs001"
    val param: Parameter[Int]    = KeyType.IntKey.make("encoder").set(22)
    val setup: Setup             = Setup(commandInfo, prefix, Set(param))

    consumeMsgsTillRunningState()

    supervisorRef ! Oneway(setup, TestProbe[CommandResponse].ref)

    val commandCurrentState = compStateProbe.expectMsgType[CurrentState]
    val commandDemandState  = DemandState(prefix, Set(choiceKey.set(commandChoice)))
    DemandMatcher(commandDemandState).check(commandCurrentState) shouldBe true
  }

  test(
    "onGoOffline and goOnline hooks of hcd comp handlers should be invoked when supervisor receives Lifecycle messages"
  ) {

    consumeMsgsTillRunningState()

    supervisorRef ! Lifecycle(GoOffline)

    val offlineCurrentState = compStateProbe.expectMsgType[CurrentState]
    val offlineDemandState  = DemandState(prefix, Set(choiceKey.set(offlineChoice)))
    DemandMatcher(offlineDemandState).check(offlineCurrentState) shouldBe true

    supervisorRef ! Lifecycle(GoOnline)

    val onlineCurrentState = compStateProbe.expectMsgType[CurrentState]
    val onlineDemandState  = DemandState(prefix, Set(choiceKey.set(onlineChoice)))
    DemandMatcher(onlineDemandState).check(onlineCurrentState) shouldBe true
  }

  test("running Hcd component should ignore RunOnline lifecycle message when it is already online") {
    consumeMsgsTillRunningState()

    supervisorRef ! Lifecycle(GoOnline)
    compStateProbe.expectNoMsg(1.seconds)
  }

  test("running Hcd component should ignore RunOffline lifecycle message when it is already offline") {
    consumeMsgsTillRunningState()

    supervisorRef ! Lifecycle(GoOffline)
    compStateProbe.expectMsgType[CurrentState]

    supervisorRef ! Lifecycle(GoOffline)
    compStateProbe.expectNoMsg(1.seconds)
  }

  test("onShutdown hook of hcd comp handlers should be invoked when supervisor receives Shutdown message") {

    consumeMsgsTillRunningState()

    supervisorRef ! Lifecycle(ToComponentLifecycleMessage.Shutdown)

    val shutdownCurrentState = compStateProbe.expectMsgType[CurrentState]
    val shutdownDemandState  = DemandState(prefix, Set(choiceKey.set(shutdownChoice)))
    DemandMatcher(shutdownDemandState).check(shutdownCurrentState) shouldBe true
  }

  test("onRestart hook of hcd comp handlers should be invoked when supervisor receives Restart message") {

    consumeMsgsTillRunningState()

    supervisorRef ! Lifecycle(Restart)

    val restartCurrentState = compStateProbe.expectMsgType[CurrentState]
    val restartDemandState  = DemandState(prefix, Set(choiceKey.set(restartChoice)))
    DemandMatcher(restartDemandState).check(restartCurrentState) shouldBe true
  }

}
