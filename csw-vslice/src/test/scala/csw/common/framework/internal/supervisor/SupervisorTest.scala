package csw.common.framework.internal.supervisor

import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.testkit.{Inbox, StubbedActorContext}
import csw.common.components.hcd.HcdDomainMsg
import csw.common.framework.models.CommonSupervisorMsg.{ComponentStateSubscription, LifecycleStateSubscription}
import csw.common.framework.models.InitialMsg.Run
import csw.common.framework.models.LifecycleState.LifecycleRunning
import csw.common.framework.models.PubSub.{Publish, Subscribe}
import csw.common.framework.models.SupervisorIdleMsg.{InitializeFailure, Initialized, Running}
import csw.common.framework.models._
import csw.common.framework.scaladsl.{ComponentHandlers, FrameworkComponentTestSuite}
import csw.param.states.CurrentState
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSuiteLike, Matchers}

class SupervisorTest
    extends FrameworkComponentTestSuite
    with FunSuiteLike
    with Matchers
    with MockitoSugar
    with BeforeAndAfterEach {

  class TestData {
    val sampleHcdHandler: ComponentHandlers[HcdDomainMsg]               = mock[ComponentHandlers[HcdDomainMsg]]
    val ctx                                                             = new StubbedActorContext[SupervisorMsg]("test-supervisor", 100, system)
    val supervisor                                                      = new Supervisor(ctx, hcdInfo, getSampleHcdFactory(sampleHcdHandler))
    val childComponentInbox: Inbox[ComponentMsg]                        = ctx.childInbox(supervisor.component.upcast)
    val childPubSubLifecycleInbox: Inbox[PubSub[LifecycleStateChanged]] = ctx.childInbox(supervisor.pubSubLifecycle)
    val childPubSubCompStateInbox: Inbox[PubSub[CurrentState]]          = ctx.childInbox(supervisor.pubSubComponent)
  }

  test("supervisor should start in Idle mode and spawn three actors") {
    val testData = new TestData
    import testData._

    supervisor.mode shouldBe SupervisorMode.Idle
    ctx.children.toList.length shouldBe 3
  }

  test("supervisor should accept Initialized message and send message to TLA") {
    val testData = new TestData
    import testData._

    supervisor.onMessage(Initialized(childComponentInbox.ref.upcast))
    childComponentInbox.receiveMsg() shouldBe Run
    supervisor.mode shouldBe SupervisorMode.Idle
  }

  test("supervisor should accept InitializeFailure message and change its mode") {
    val testData = new TestData
    import testData._

    supervisor.onMessage(InitializeFailure("test messge for initialization failure"))
    supervisor.mode shouldBe SupervisorMode.LifecycleFailure
  }

  test("supervisor should accept Running message from component and change its mode and publish state change") {
    val testData = new TestData
    import testData._

    supervisor.onMessage(Running(childComponentInbox.ref))

    supervisor.mode shouldBe SupervisorMode.Running
    childPubSubLifecycleInbox.receiveMsg() shouldBe Publish(LifecycleStateChanged(LifecycleRunning))
  }

  test("supervisor should handle LifecycleStateSubscription message by coordinating with pub sub actor") {
    val testData = new TestData
    import testData._

    val previousSupervisorMode = supervisor.mode
    val subscriberProbe        = TestProbe[LifecycleStateChanged]

    supervisor.onMessage(LifecycleStateSubscription(Subscribe[LifecycleStateChanged](subscriberProbe.ref)))

    supervisor.mode shouldBe previousSupervisorMode

    val message = childPubSubLifecycleInbox.receiveMsg()
    message shouldBe Subscribe[LifecycleStateChanged](subscriberProbe.ref)
  }

  test("supervisor should handle ComponentStateSubscription message by coordinating with pub sub actor") {
    val testData = new TestData
    import testData._

    val subscriberProbe        = TestProbe[CurrentState]
    val previousSupervisorMode = testData.supervisor.mode

    supervisor.onMessage(ComponentStateSubscription(Subscribe[CurrentState](subscriberProbe.ref)))

    supervisor.mode shouldBe previousSupervisorMode

    val message = childPubSubCompStateInbox.receiveMsg()
    message shouldBe Subscribe[CurrentState](subscriberProbe.ref)
  }

}
