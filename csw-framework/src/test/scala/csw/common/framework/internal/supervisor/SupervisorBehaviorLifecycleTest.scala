package csw.common.framework.internal.supervisor

import akka.typed.scaladsl.TimerScheduler
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.testkit.{Inbox, StubbedActorContext}
import csw.common.components.ComponentDomainMessage
import csw.common.framework.ComponentInfos._
import csw.common.framework.FrameworkTestMocks.TypedActorMock
import csw.common.framework.internal.pubsub.PubSubBehaviorFactory
import csw.common.framework.models.FromComponentLifecycleMessage.Running
import csw.common.framework.models.FromSupervisorMessage.SupervisorLifecycleStateChanged
import csw.common.framework.models.PubSub.{Publish, Subscribe, Unsubscribe}
import csw.common.framework.models.RunningMessage.{DomainMessage, Lifecycle}
import csw.common.framework.models.SupervisorCommonMessage.{ComponentStateSubscription, LifecycleStateSubscription}
import csw.common.framework.models.SupervisorIdleMessage.{InitializeTimeout, RegistrationComplete}
import csw.common.framework.models.{ToComponentLifecycleMessage, _}
import csw.common.framework.scaladsl.ComponentHandlers
import csw.common.framework.{FrameworkTestMocks, FrameworkTestSuite}
import csw.param.states.CurrentState
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach

// DEOPSCSW-163: Provide admin facilities in the framework through Supervisor role
// DEOPSCSW-177: Hooks for lifecycle management
class SupervisorBehaviorLifecycleTest extends FrameworkTestSuite with BeforeAndAfterEach {

  class TestData() {
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
        Some(containerIdleMessageProbe.ref),
        hcdInfo,
        getSampleHcdWiring(sampleHcdHandler),
        new PubSubBehaviorFactory,
        registrationFactory,
        locationService
      ) with TypedActorMock[SupervisorMessage]
    supervisor.onMessage(RegistrationComplete(registrationResult))
    verify(timer).startSingleTimer(
      SupervisorBehavior.InitializeTimerKey,
      InitializeTimeout,
      SupervisorBehavior.initializeTimeout
    )
    val childComponentInbox: Inbox[ComponentMessage]                    = ctx.childInbox(supervisor.component.upcast)
    val childPubSubLifecycleInbox: Inbox[PubSub[LifecycleStateChanged]] = ctx.childInbox(supervisor.pubSubLifecycle)
    val childPubSubCompStateInbox: Inbox[PubSub[CurrentState]]          = ctx.childInbox(supervisor.pubSubComponent)
  }

  test("supervisor should start in Idle lifecycle state and spawn three actors") {
    val testData = new TestData()
    import testData._

    supervisor.lifecycleState shouldBe SupervisorLifecycleState.Idle
    ctx.children.size shouldBe 3

  }

  // *************** Begin testing of onIdleMessages ***************
  test(
    "supervisor should accept Running message from component and change its lifecycle state and publish state change"
  ) {
    val testData = new TestData()
    import testData._

    supervisor.runningComponent shouldBe empty
    supervisor.onMessage(Running(childComponentInbox.ref))

    verify(timer).cancel(SupervisorBehavior.InitializeTimerKey)
    supervisor.lifecycleState shouldBe SupervisorLifecycleState.Running
    supervisor.runningComponent shouldBe Some(childComponentInbox.ref)
    containerIdleMessageProbe.expectMsg(
      SupervisorLifecycleStateChanged(ctx.selfInbox.ref, SupervisorLifecycleState.Running)
    )
    childPubSubLifecycleInbox.receiveMsg() shouldBe Publish(
      LifecycleStateChanged(ctx.self, SupervisorLifecycleState.Running)
    )
  }
  // *************** End of testing onIdleMessages ***************

  // *************** Begin testing of onCommonMessages ***************
  test("supervisor should handle LifecycleStateSubscription message by coordinating with pub sub actor") {
    val testData = new TestData()
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
    val testData = new TestData()
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
    val testData = new TestData()
    import testData._

    supervisor.onMessage(Running(childComponentInbox.ref))
    supervisor.onMessage(Restart)
    supervisor.lifecycleState shouldBe SupervisorLifecycleState.Restart
  }

  test("supervisor should handle lifecycle GoOffline message") {
    val testData = new TestData()
    import testData._

    supervisor.onMessage(Running(childComponentInbox.ref))
    supervisor.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOffline))
    supervisor.lifecycleState shouldBe SupervisorLifecycleState.RunningOffline
    childComponentInbox.receiveAll() should contain(Lifecycle(ToComponentLifecycleMessage.GoOffline))
  }

  test("supervisor should handle lifecycle GoOnline message") {
    val testData = new TestData()
    import testData._

    supervisor.onMessage(Running(childComponentInbox.ref))
    supervisor.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOffline))
    supervisor.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOnline))
    supervisor.lifecycleState shouldBe SupervisorLifecycleState.Running
    childComponentInbox.receiveAll() should contain(Lifecycle(ToComponentLifecycleMessage.GoOnline))
  }

  test("supervisor should accept and forward Domain message to a TLA") {
    val testData = new TestData()
    import testData._

    sealed trait TestDomainMessage extends DomainMessage
    case object TestCompMessage$   extends TestDomainMessage

    supervisor.onMessage(Running(childComponentInbox.ref))
    supervisor.onMessage(TestCompMessage$)
    childComponentInbox.receiveMsg() shouldBe TestCompMessage$
  }
  // *************** End of testing onRunning Messages ***************

  test("should not forward Domain message to a TLA when supervisor is in Idle lifecycle state") {
    val testData = new TestData()
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
    val testData = new TestData()
    import testData._

    sealed trait TestDomainMessage extends DomainMessage
    case object TestCompMessage    extends TestDomainMessage

    supervisor.lifecycleState shouldBe SupervisorLifecycleState.Idle
    supervisor.onMessage(Running(childComponentInbox.ref))
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
}
