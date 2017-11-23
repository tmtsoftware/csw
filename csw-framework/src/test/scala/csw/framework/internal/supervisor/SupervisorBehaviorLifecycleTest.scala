package csw.framework.internal.supervisor

import akka.typed.Terminated
import akka.typed.scaladsl.TimerScheduler
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.testkit.{Inbox, StubbedActorContext}
import csw.common.components.framework.ComponentDomainMessage
import csw.exceptions.{FailureStop, InitializationFailed}
import csw.framework.ComponentInfos._
import csw.framework.FrameworkTestMocks.MutableActorMock
import csw.framework.internal.pubsub.PubSubBehaviorFactory
import csw.framework.scaladsl.ComponentHandlers
import csw.framework.{FrameworkTestMocks, FrameworkTestSuite}
import csw.messages.CommandResponseManagerMessage.Query
import csw.messages.FromComponentLifecycleMessage.Running
import csw.messages.RunningMessage.{DomainMessage, Lifecycle}
import csw.messages.SupervisorCommonMessage.{ComponentStateSubscription, LifecycleStateSubscription}
import csw.messages.SupervisorIdleMessage.InitializeTimeout
import csw.messages.SupervisorInternalRunningMessage.{RegistrationNotRequired, RegistrationSuccess}
import csw.messages.ccs.commands.CommandResponse
import csw.messages.framework.LocationServiceUsage.DoNotRegister
import csw.messages.framework.{ComponentInfo, SupervisorLifecycleState}
import csw.messages.models.PubSub.{Publish, Subscribe, Unsubscribe}
import csw.messages.models.{LifecycleStateChanged, PubSub, ToComponentLifecycleMessage}
import csw.messages.params.models.RunId
import csw.messages.params.states.CurrentState
import csw.messages.{models, _}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach

// DEOPSCSW-163: Provide admin facilities in the framework through Supervisor role
// DEOPSCSW-177: Hooks for lifecycle management
class SupervisorBehaviorLifecycleTest extends FrameworkTestSuite with BeforeAndAfterEach {

  class TestData(compInfo: ComponentInfo) {
    val testMocks: FrameworkTestMocks = frameworkTestMocks()
    import testMocks._

    val sampleHcdHandler: ComponentHandlers[ComponentDomainMessage] = mock[ComponentHandlers[ComponentDomainMessage]]
    val ctx                                                         = new StubbedActorContext[SupervisorMessage]("test-supervisor", 100, system)
    val timer: TimerScheduler[SupervisorMessage]                    = mock[TimerScheduler[SupervisorMessage]]
    val containerIdleMessageProbe: TestProbe[ContainerIdleMessage]  = TestProbe[ContainerIdleMessage]
    val supervisor =
      new SupervisorBehavior(
        ctx,
        timer,
        None,
        compInfo,
        getSampleHcdWiring(sampleHcdHandler),
        new PubSubBehaviorFactory,
        registrationFactory,
        locationService
      ) with MutableActorMock[SupervisorMessage]

    verify(timer).startSingleTimer(SupervisorBehavior.InitializeTimerKey, InitializeTimeout, supervisor.initializeTimeout)

    val childComponentInbox: Inbox[ComponentMessage]                    = ctx.childInbox(supervisor.component.get.upcast)
    val childPubSubLifecycleInbox: Inbox[PubSub[LifecycleStateChanged]] = ctx.childInbox(supervisor.pubSubLifecycle)
    val childPubSubCompStateInbox: Inbox[PubSub[CurrentState]]          = ctx.childInbox(supervisor.pubSubComponent)
    val childCmdResponseMgrInbox: Inbox[CommandResponseManagerMessage]  = ctx.childInbox(supervisor.commandResponseManager)
  }

  test("supervisor should start in Idle lifecycle state and spawn four actors") {
    val testData = new TestData(hcdInfo)
    import testData._

    supervisor.lifecycleState shouldBe SupervisorLifecycleState.Idle
    ctx.children.size shouldBe 4
    verify(timer).startSingleTimer(SupervisorBehavior.InitializeTimerKey, InitializeTimeout, supervisor.initializeTimeout)
  }

  // *************** Begin testing of onIdleMessages ***************
  test("supervisor should accept Running message and register with Location service when LocationServiceUsage is RegisterOnly") {
    val testData = new TestData(hcdInfo)
    import testData._
    import testData.testMocks._

    val childRef = childComponentInbox.ref.upcast

    supervisor.onMessage(Running(childRef))

    verify(timer).cancel(SupervisorBehavior.InitializeTimerKey)
    verify(locationService).register(akkaRegistration)

    supervisor.lifecycleState shouldBe SupervisorLifecycleState.Running
  }

  test("supervisor should accept Running message and should not register when LocationServiceUsage is DoNotRegister") {
    val testData = new TestData(hcdInfo.copy(locationServiceUsage = DoNotRegister))
    import testData._
    import testData.testMocks._

    val childRef = childComponentInbox.ref.upcast

    supervisor.onMessage(Running(childRef))

    verify(locationService, never()).register(akkaRegistration)
    verify(timer).cancel(SupervisorBehavior.InitializeTimerKey)
    supervisor.onMessage(RegistrationNotRequired(childRef))

    supervisor.lifecycleState shouldBe SupervisorLifecycleState.Running
  }
  // *************** End of testing onIdleMessages ***************

  // *************** Begin testing of onInternalMessage ***************
  test("supervisor should publish state change after successful registration with location service") {
    val testData = new TestData(hcdInfo)
    import testData._

    val childRef = childComponentInbox.ref.upcast

    supervisor.onMessage(Running(childRef))
    supervisor.onMessage(RegistrationSuccess(childRef))

    childPubSubLifecycleInbox.receiveMsg() shouldBe Publish(
      models.LifecycleStateChanged(ctx.self, SupervisorLifecycleState.Running)
    )
  }

  test("supervisor should publish state change if locationServiceUsage is DoNotRegister") {
    val testData = new TestData(hcdInfo)
    import testData._
    val childRef = childComponentInbox.ref.upcast

    supervisor.onMessage(Running(childRef))
    supervisor.onMessage(RegistrationNotRequired(childRef))

    supervisor.lifecycleState shouldBe SupervisorLifecycleState.Running
    childPubSubLifecycleInbox.receiveMsg() shouldBe Publish(
      models.LifecycleStateChanged(ctx.self, SupervisorLifecycleState.Running)
    )
  }
  // *************** End of testing onInternalMessage ***************

  // *************** Begin testing of onCommonMessages ***************
  test("supervisor should handle LifecycleStateSubscription message by coordinating with pub sub actor") {
    val testData = new TestData(hcdInfo)
    import testData._

    val previousSupervisorLifecyleState = supervisor.lifecycleState
    val subscriberProbe                 = TestProbe[LifecycleStateChanged]

    // Subscribe
    supervisor.onMessage(LifecycleStateSubscription(Subscribe(subscriberProbe.ref)))
    supervisor.lifecycleState shouldBe previousSupervisorLifecyleState
    val subscribeMessage = childPubSubLifecycleInbox.receiveMsg()
    subscribeMessage shouldBe Subscribe[LifecycleStateChanged](subscriberProbe.ref)

    // Unsubscribe
    supervisor.onMessage(LifecycleStateSubscription(Unsubscribe[LifecycleStateChanged](subscriberProbe.ref)))
    supervisor.lifecycleState shouldBe previousSupervisorLifecyleState
    val unsubscribeMessage = childPubSubLifecycleInbox.receiveMsg()
    unsubscribeMessage shouldBe Unsubscribe[LifecycleStateChanged](subscriberProbe.ref)
  }

  test("supervisor should handle ComponentStateSubscription message by coordinating with pub sub actor") {
    val testData = new TestData(hcdInfo)
    import testData._

    val subscriberProbe                 = TestProbe[CurrentState]
    val previousSupervisorLifecyleState = testData.supervisor.lifecycleState

    // Subscribe
    supervisor.onMessage(ComponentStateSubscription(Subscribe[CurrentState](subscriberProbe.ref)))
    supervisor.lifecycleState shouldBe previousSupervisorLifecyleState
    val subscribeMessage = childPubSubCompStateInbox.receiveMsg()
    subscribeMessage shouldBe Subscribe[CurrentState](subscriberProbe.ref)

    // Unsubscribe
    supervisor.onMessage(ComponentStateSubscription(Unsubscribe[CurrentState](subscriberProbe.ref)))
    supervisor.lifecycleState shouldBe previousSupervisorLifecyleState
    val unsubscribeMessage = childPubSubCompStateInbox.receiveMsg()
    unsubscribeMessage shouldBe Unsubscribe[CurrentState](subscriberProbe.ref)
  }

  // *************** End of testing onCommonMessages ***************

  /**
   * Below Tests show that all external messages for the TLA are received by the Supervisor
   * which passes them to TLA (depending on lifecycle)
  **/
  // *************** Begin testing of onRunning Messages ***************

  test("supervisor should handle lifecycle Restart message") {
    val testData = new TestData(hcdInfo)
    import testData._

    supervisor.onMessage(Restart)
    supervisor.lifecycleState shouldBe SupervisorLifecycleState.Restart
  }

  test("supervisor should handle lifecycle GoOffline message") {
    val testData = new TestData(hcdInfo)
    import testData._

    val childRef = childComponentInbox.ref.upcast

    supervisor.onMessage(Running(childRef))
    supervisor.onMessage(RegistrationSuccess(childRef))
    supervisor.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOffline))
    supervisor.lifecycleState shouldBe SupervisorLifecycleState.RunningOffline
    childComponentInbox.receiveAll() should contain(Lifecycle(ToComponentLifecycleMessage.GoOffline))
  }

  test("supervisor should handle lifecycle GoOnline message") {
    val testData = new TestData(hcdInfo)
    import testData._

    val childRef = childComponentInbox.ref.upcast

    supervisor.onMessage(Running(childRef))
    supervisor.onMessage(RegistrationSuccess(childRef))
    supervisor.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOffline))
    supervisor.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOnline))
    supervisor.lifecycleState shouldBe SupervisorLifecycleState.Running
    childComponentInbox.receiveAll() should contain(Lifecycle(ToComponentLifecycleMessage.GoOnline))
  }

  test("supervisor should accept and forward Domain message to a TLA") {
    val testData = new TestData(hcdInfo)
    import testData._

    sealed trait TestDomainMessage extends DomainMessage
    case object TestCompMessage$   extends TestDomainMessage

    val childRef = childComponentInbox.ref.upcast

    supervisor.onMessage(Running(childRef))
    supervisor.onMessage(RegistrationSuccess(childRef))
    supervisor.onMessage(TestCompMessage$)
    childComponentInbox.receiveMsg() shouldBe TestCompMessage$
  }
  // *************** End of testing onRunning Messages ***************

  test("should not forward Domain message to a TLA when supervisor is in Idle lifecycle state") {
    val testData = new TestData(hcdInfo)
    import testData._

    sealed trait TestDomainMessage extends DomainMessage
    case object TestCompMessage    extends TestDomainMessage

    // TestCompMessage (DomainMessage) is sent to supervisor when lifecycle state is Idle
    // in this case, verify that log.error is called once
    supervisor.lifecycleState shouldBe SupervisorLifecycleState.Idle
    supervisor.onMessage(TestCompMessage)
    supervisor.runningComponent shouldBe empty
    verify(supervisor.log, times(1)).error(ArgumentMatchers.any())
  }

  test("should not forward Domain message to a TLA when supervisor is in Restart lifecycle state") {
    val testData = new TestData(hcdInfo)
    import testData._
    val childRef = childComponentInbox.ref.upcast

    sealed trait TestDomainMessage extends DomainMessage
    case object TestCompMessage    extends TestDomainMessage

    supervisor.lifecycleState shouldBe SupervisorLifecycleState.Idle

    supervisor.onMessage(Running(childRef))
    supervisor.onMessage(RegistrationSuccess(childRef))
    supervisor.lifecycleState shouldBe SupervisorLifecycleState.Running

    // TestCompMessage (DomainMessage) is sent to supervisor when lifecycle state is running
    // in this case, verify that log.error is never called
    supervisor.onMessage(TestCompMessage)
    verify(supervisor.log, never()).error(ArgumentMatchers.any())
    childComponentInbox.receiveMsg() shouldBe TestCompMessage

    // TestCompMessage (DomainMessage) is sent to supervisor when lifecycle state is Restart
    // in this case, verify that log.error is called once and
    // TLA does not receive any message
    supervisor.onMessage(Restart)
    supervisor.lifecycleState shouldBe SupervisorLifecycleState.Restart
    supervisor.onMessage(TestCompMessage)
    childComponentInbox.receiveAll() shouldBe Seq.empty
    verify(supervisor.log, times(1)).error(ArgumentMatchers.any())
  }

  test("supervisor should handle Terminated signal for Idle lifecycle state") {
    val testData = new TestData(hcdInfo)
    import testData._

    supervisor.lifecycleState shouldBe SupervisorLifecycleState.Idle
    intercept[InitializationFailed.type] {
      supervisor.onSignal(Terminated(childComponentInbox.ref)(FailureStop("reason of failing")))
    }
  }

  test("supervisor should handle Command Response Manager messages by forwarding it to Command Response Manager") {
    val testData = new TestData(hcdInfo)
    import testData._

    val childRef        = childComponentInbox.ref.upcast
    val subscriberProbe = TestProbe[CommandResponse]
    val testCmdId       = RunId()

    supervisor.onMessage(Running(childRef))

    supervisor.onMessage(Query(testCmdId, subscriberProbe.ref))
    childCmdResponseMgrInbox.receiveMsg() shouldBe Query(testCmdId, subscriberProbe.ref)

    supervisor.onMessage(CommandResponseManagerMessage.Subscribe(testCmdId, subscriberProbe.ref))
    childCmdResponseMgrInbox.receiveMsg() shouldBe CommandResponseManagerMessage.Subscribe(testCmdId, subscriberProbe.ref)

    supervisor.onMessage(CommandResponseManagerMessage.Unsubscribe(testCmdId, subscriberProbe.ref))
    childCmdResponseMgrInbox.receiveMsg() shouldBe CommandResponseManagerMessage.Unsubscribe(testCmdId, subscriberProbe.ref)
  }
}
