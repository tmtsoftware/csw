package csw.common.framework.internal.supervisor

import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.testkit.{Inbox, StubbedActorContext}
import csw.common.components.ComponentDomainMsg
import csw.common.framework.FrameworkComponentTestInfos._
import csw.common.framework.FrameworkComponentTestSuite
import csw.common.framework.internal.{Supervisor, SupervisorMode}
import csw.common.framework.models.CommonSupervisorMsg.{
  ComponentStateSubscription,
  HaltComponent,
  LifecycleStateSubscription
}
import csw.common.framework.models.ContainerMsg.LifecycleStateChanged
import csw.common.framework.models.InitialMsg.Run
import csw.common.framework.models.PreparingToShutdownMsg.{ShutdownComplete, ShutdownFailure, ShutdownTimeout}
import csw.common.framework.models.PubSub.{Publish, Subscribe, Unsubscribe}
import csw.common.framework.models.RunningMsg.{DomainMsg, Lifecycle}
import csw.common.framework.models.SupervisorIdleMsg.{InitializeFailure, Initialized, Running}
import csw.common.framework.models.{ToComponentLifecycleMessage, _}
import csw.common.framework.scaladsl.ComponentHandlers
import csw.param.states.CurrentState
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar

// DEOPSCSW-163: Provide admin facilities in the framework through Supervisor role
class SupervisorLifecycleTest extends FrameworkComponentTestSuite with MockitoSugar with BeforeAndAfterEach {

  class TestData {
    val sampleHcdHandler: ComponentHandlers[ComponentDomainMsg]         = mock[ComponentHandlers[ComponentDomainMsg]]
    val ctx                                                             = new StubbedActorContext[SupervisorMsg]("test-supervisor", 100, system)
    val supervisor                                                      = new Supervisor(ctx, hcdInfo, getSampleHcdWiring(sampleHcdHandler))
    val childComponentInbox: Inbox[ComponentMsg]                        = ctx.childInbox(supervisor.component.upcast)
    val childPubSubLifecycleInbox: Inbox[PubSub[LifecycleStateChanged]] = ctx.childInbox(supervisor.pubSubLifecycle)
    val childPubSubCompStateInbox: Inbox[PubSub[CurrentState]]          = ctx.childInbox(supervisor.pubSubComponent)
  }

  test("supervisor should start in Idle mode and spawn three actors") {
    val testData = new TestData
    import testData._

    supervisor.mode shouldBe SupervisorMode.Idle
    ctx.children.size shouldBe 3
  }

  // *************** Begin testing of onIdleMessages ***************
  test("supervisor should accept Initialized message and send Run message to TLA") {
    val testData = new TestData
    import testData._

    supervisor.onMessage(Initialized(childComponentInbox.ref.upcast))
    childComponentInbox.receiveMsg() shouldBe Run
    supervisor.mode shouldBe SupervisorMode.Idle
  }

  test("supervisor should accept InitializeFailure message and change its mode") {
    val testData = new TestData
    import testData._

    supervisor.onMessage(InitializeFailure("test message for initialization failure"))
    supervisor.mode shouldBe SupervisorMode.InitializeFailure
  }

  test("supervisor should accept Running message from component and change its mode and publish state change") {
    val testData = new TestData
    import testData._

    supervisor.onMessage(Running(childComponentInbox.ref))

    supervisor.mode shouldBe SupervisorMode.Running
    childPubSubLifecycleInbox.receiveMsg() shouldBe Publish(LifecycleStateChanged(SupervisorMode.Running, ctx.self))
  }
  // *************** End of testing onIdleMessages ***************

  // *************** Begin testing of onCommonMessages ***************
  test("supervisor should handle LifecycleStateSubscription message by coordinating with pub sub actor") {
    val testData = new TestData
    import testData._

    val previousSupervisorMode = supervisor.mode
    val subscriberProbe        = TestProbe[LifecycleStateChanged]

    // Subscribe
    supervisor.onMessage(LifecycleStateSubscription(Subscribe(subscriberProbe.ref)))
    supervisor.mode shouldBe previousSupervisorMode
    val subscribeMessage = childPubSubLifecycleInbox.receiveMsg()
    subscribeMessage shouldBe Subscribe[LifecycleStateChanged](subscriberProbe.ref)

    // Unsubscribe
    supervisor.onMessage(LifecycleStateSubscription(Unsubscribe[LifecycleStateChanged](subscriberProbe.ref)))
    supervisor.mode shouldBe previousSupervisorMode
    val unsubscribeMessage = childPubSubLifecycleInbox.receiveMsg()
    unsubscribeMessage shouldBe Unsubscribe[LifecycleStateChanged](subscriberProbe.ref)
  }

  test("supervisor should handle ComponentStateSubscription message by coordinating with pub sub actor") {
    val testData = new TestData
    import testData._

    val subscriberProbe        = TestProbe[CurrentState]
    val previousSupervisorMode = testData.supervisor.mode

    // Subscribe
    supervisor.onMessage(ComponentStateSubscription(Subscribe[CurrentState](subscriberProbe.ref)))
    supervisor.mode shouldBe previousSupervisorMode
    val subscribeMessage = childPubSubCompStateInbox.receiveMsg()
    subscribeMessage shouldBe Subscribe[CurrentState](subscriberProbe.ref)

    // Unsubscribe
    supervisor.onMessage(ComponentStateSubscription(Unsubscribe[CurrentState](subscriberProbe.ref)))
    supervisor.mode shouldBe previousSupervisorMode
    val unsubscribeMessage = childPubSubCompStateInbox.receiveMsg()
    unsubscribeMessage shouldBe Unsubscribe[CurrentState](subscriberProbe.ref)
  }

  // Fixme
  ignore("supervisor should handle HaltComponent message by shutting down all child actors in all the mode") {
    val testData = new TestData
    import testData._

    // put supervisor in InitializeFailure mode
    val initialMode = SupervisorMode.InitializeFailure
    supervisor.onMessage(InitializeFailure("Unexpected error"))
    supervisor.mode shouldBe initialMode

    // HaltComponent
    supervisor.onMessage(HaltComponent)
    supervisor.mode shouldBe initialMode
    supervisor.haltingFlag shouldBe true
    ctx.selfInbox.receiveMsg() shouldBe Lifecycle(ToComponentLifecycleMessage.Shutdown)

    ctx.watch(supervisor.component)
    ctx.watch(supervisor.pubSubComponent)
    ctx.watch(supervisor.pubSubLifecycle)

    // HaltComponent schedules Shutdown message to self
    supervisor.onMessage(Lifecycle(ToComponentLifecycleMessage.Shutdown))
    supervisor.mode shouldBe SupervisorMode.PreparingToShutdown

    supervisor.onMessage(ShutdownComplete)
    supervisor.mode shouldBe SupervisorMode.Shutdown

  }
  // *************** End of testing onCommonMessages ***************

  /**
   * Below Tests show that all external messages for the TLA are received by the Supervisor
   * which passes them to TLA (depending on lifecycle)
  **/
  // *************** Begin testing of onRunning Messages ***************
  test("supervisor should handle lifecycle Shutdown message") {
    val testData = new TestData
    import testData._

    supervisor.onMessage(Running(childComponentInbox.ref))
    supervisor.onMessage(Lifecycle(ToComponentLifecycleMessage.Shutdown))
    childPubSubLifecycleInbox.receiveAll() should contain(
      Publish(LifecycleStateChanged(SupervisorMode.PreparingToShutdown, ctx.self))
    )
    supervisor.mode shouldBe SupervisorMode.PreparingToShutdown
    childComponentInbox.receiveAll() should contain(Lifecycle(ToComponentLifecycleMessage.Shutdown))
  }

  test("supervisor should handle lifecycle Restart message") {
    val testData = new TestData
    import testData._

    supervisor.onMessage(Running(childComponentInbox.ref))
    supervisor.onMessage(Lifecycle(ToComponentLifecycleMessage.Restart))
    supervisor.mode shouldBe SupervisorMode.Idle
    childComponentInbox.receiveAll() should contain(Lifecycle(ToComponentLifecycleMessage.Restart))
  }

  test("supervisor should handle lifecycle GoOffline message") {
    val testData = new TestData
    import testData._

    supervisor.onMessage(Running(childComponentInbox.ref))
    supervisor.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOffline))
    supervisor.mode shouldBe SupervisorMode.RunningOffline
    childComponentInbox.receiveAll() should contain(Lifecycle(ToComponentLifecycleMessage.GoOffline))
  }

  test("supervisor should handle lifecycle GoOnline message") {
    val testData = new TestData
    import testData._

    supervisor.onMessage(Running(childComponentInbox.ref))
    supervisor.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOffline))
    supervisor.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOnline))
    supervisor.mode shouldBe SupervisorMode.Running
    childComponentInbox.receiveAll() should contain(Lifecycle(ToComponentLifecycleMessage.GoOnline))
  }

  test("supervisor should accept and forward Domain message to a TLA") {
    val testData = new TestData
    import testData._

    sealed trait TestDomainMsg extends DomainMsg
    case object TestCompMsg    extends TestDomainMsg

    supervisor.onMessage(Running(childComponentInbox.ref))
    supervisor.onMessage(TestCompMsg)
    childComponentInbox.receiveMsg() shouldBe TestCompMsg
  }
  // *************** End of testing onRunning Messages ***************

  // *************** Begin testing of onPreparingToShutdown Messages ***************
  test("supervisor should handle ShutdownTimeout message from TLA") {
    val testData = new TestData
    import testData._

    supervisor.onMessage(Running(childComponentInbox.ref))

    supervisor.shutdownTimer.isDefined shouldBe false
    supervisor.onMessage(Lifecycle(ToComponentLifecycleMessage.Shutdown))

    supervisor.shutdownTimer.isDefined shouldBe true

    supervisor.mode shouldBe SupervisorMode.PreparingToShutdown
    childPubSubLifecycleInbox.receiveAll() should contain(
      Publish(LifecycleStateChanged(SupervisorMode.PreparingToShutdown, ctx.self))
    )

    supervisor.onMessage(ShutdownTimeout)
    supervisor.shutdownTimer.get.isCancelled shouldBe true

    supervisor.mode shouldBe SupervisorMode.ShutdownFailure
    childPubSubLifecycleInbox.receiveAll() should contain(
      Publish(LifecycleStateChanged(SupervisorMode.ShutdownFailure, ctx.self))
    )
  }

  test("supervisor should handle ShutdownFailure message from TLA") {
    val testData = new TestData
    import testData._

    supervisor.onMessage(Running(childComponentInbox.ref))

    supervisor.shutdownTimer.isDefined shouldBe false
    supervisor.onMessage(Lifecycle(ToComponentLifecycleMessage.Shutdown))

    supervisor.shutdownTimer.isDefined shouldBe true

    supervisor.mode shouldBe SupervisorMode.PreparingToShutdown
    childPubSubLifecycleInbox.receiveAll() should contain(
      Publish(LifecycleStateChanged(SupervisorMode.PreparingToShutdown, ctx.self))
    )

    supervisor.onMessage(ShutdownFailure("Exception occurred"))
    supervisor.shutdownTimer.get.isCancelled shouldBe true

    supervisor.mode shouldBe SupervisorMode.ShutdownFailure
    childPubSubLifecycleInbox.receiveAll() should contain(
      Publish(LifecycleStateChanged(SupervisorMode.ShutdownFailure, ctx.self))
    )
  }

  test("supervisor should handle ShutdownComplete message from TLA") {
    val testData = new TestData
    import testData._

    supervisor.onMessage(Running(childComponentInbox.ref))

    supervisor.shutdownTimer.isDefined shouldBe false
    supervisor.onMessage(Lifecycle(ToComponentLifecycleMessage.Shutdown))

    supervisor.shutdownTimer.isDefined shouldBe true

    supervisor.mode shouldBe SupervisorMode.PreparingToShutdown
    childPubSubLifecycleInbox.receiveAll() should contain(
      Publish(LifecycleStateChanged(SupervisorMode.PreparingToShutdown, ctx.self))
    )

    supervisor.onMessage(ShutdownComplete)
    supervisor.shutdownTimer.get.isCancelled shouldBe true

    supervisor.mode shouldBe SupervisorMode.Shutdown
    childPubSubLifecycleInbox.receiveAll() should contain(
      Publish(LifecycleStateChanged(SupervisorMode.Shutdown, ctx.self))
    )
  }
  // *************** End of testing onPreparingToShutdown Messages ***************
}
