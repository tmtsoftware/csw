package csw.common.framework.internal.supervisor

import akka.typed.scaladsl.ActorContext
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{Behavior, Props}
import csw.common.components.hcd.HcdDomainMsg
import csw.common.framework.models.CommonSupervisorMsg.{ComponentStateSubscription, LifecycleStateSubscription}
import csw.common.framework.models.InitialMsg.Run
import csw.common.framework.models.LifecycleState.LifecycleRunning
import csw.common.framework.models.PubSub.{Publish, Subscribe}
import csw.common.framework.models.SupervisorIdleMsg.{InitializeFailure, Initialized, Running}
import csw.common.framework.models._
import csw.common.framework.scaladsl.{ComponentHandlers, FrameworkComponentTestSuite}
import csw.param.states.CurrentState
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSuiteLike, Matchers}

class SupervisorTest2
    extends FrameworkComponentTestSuite
    with FunSuiteLike
    with Matchers
    with MockitoSugar
    with BeforeAndAfterEach {

  val componentProbe: TestProbe[ComponentMsg]                        = TestProbe[ComponentMsg]
  val pubSubCompStateProbe: TestProbe[PubSub[CurrentState]]          = TestProbe[PubSub[CurrentState]]
  val pubSubLifecycleProbe: TestProbe[PubSub[LifecycleStateChanged]] = TestProbe[PubSub[LifecycleStateChanged]]
  val supervisorProbe: TestProbe[SupervisorMsg]                      = TestProbe[SupervisorMsg]

  def getContext: ActorContext[SupervisorMsg] = {
    val context: ActorContext[SupervisorMsg] = mock[ActorContext[SupervisorMsg]]

    when(
      context
        .spawn(any[Behavior[PubSub[LifecycleStateChanged]]], ArgumentMatchers.eq("pub-sub-lifecycle"), any[Props]())
    ).thenReturn(pubSubLifecycleProbe.ref)

    when(context.spawn(any[Behavior[PubSub[CurrentState]]], ArgumentMatchers.eq("pub-sub-component"), any[Props]()))
      .thenReturn(pubSubCompStateProbe.ref)

    when(context.spawn[Nothing](any[Behavior[Nothing]], ArgumentMatchers.eq("component"), any[Props]()))
      .thenReturn(componentProbe.ref)

    when(context.self).thenReturn(supervisorProbe.ref)
    doNothing().when(context).watch(any())

    context
  }

  test("supervisor should start in Idle mode") {
    val sampleHcdHandler: ComponentHandlers[HcdDomainMsg] = mock[ComponentHandlers[HcdDomainMsg]]

    val supervisor = new Supervisor(getContext, hcdInfo, getSampleHcdFactory(sampleHcdHandler))
    supervisor.mode shouldBe SupervisorMode.Idle
    supervisor.pubSubLifecycle shouldBe pubSubLifecycleProbe.ref
    supervisor.component shouldBe componentProbe.ref
    supervisor.pubSubComponent shouldBe pubSubCompStateProbe.ref
  }

  test("supervisor should accept Initialized message and send message to TLA") {
    val sampleHcdHandler: ComponentHandlers[HcdDomainMsg] = mock[ComponentHandlers[HcdDomainMsg]]

    val supervisor = new Supervisor(getContext, hcdInfo, getSampleHcdFactory(sampleHcdHandler))
    supervisor.onMessage(Initialized(componentProbe.ref))
    componentProbe.expectMsgType[Run.type]
    supervisor.mode shouldBe SupervisorMode.Idle
  }

  test("supervisor should accept InitializeFailure message and change its mode") {
    val sampleHcdHandler: ComponentHandlers[HcdDomainMsg] = mock[ComponentHandlers[HcdDomainMsg]]

    val supervisor = new Supervisor(getContext, hcdInfo, getSampleHcdFactory(sampleHcdHandler))
    supervisor.onMessage(InitializeFailure("test messge for initialization failure"))
    supervisor.mode shouldBe SupervisorMode.LifecycleFailure
  }

  test("supervisor should accept Running message from component and change its mode and publish state change") {
    val sampleHcdHandler: ComponentHandlers[HcdDomainMsg] = mock[ComponentHandlers[HcdDomainMsg]]

    val supervisor = new Supervisor(getContext, hcdInfo, getSampleHcdFactory(sampleHcdHandler))
    supervisor.onMessage(Running(componentProbe.ref))
    supervisor.mode shouldBe SupervisorMode.Running

    supervisor.pubSubLifecycle shouldBe pubSubLifecycleProbe.ref
    val publishedState = pubSubLifecycleProbe.expectMsgType[Publish[LifecycleStateChanged]]
    publishedState.data shouldBe LifecycleStateChanged(LifecycleRunning)
  }

  test("supervisor should handle LifecycleStateSubscription message by coordinating with pub sub actor") {
    val sampleHcdHandler: ComponentHandlers[HcdDomainMsg] = mock[ComponentHandlers[HcdDomainMsg]]
    val subscriberProbe                                   = TestProbe[LifecycleStateChanged]

    val supervisor             = new Supervisor(getContext, hcdInfo, getSampleHcdFactory(sampleHcdHandler))
    val previousSupervisorMode = supervisor.mode
    supervisor.onMessage(LifecycleStateSubscription(Subscribe[LifecycleStateChanged](subscriberProbe.ref)))
    supervisor.mode shouldBe previousSupervisorMode

    val subscribe = pubSubLifecycleProbe.expectMsgType[Subscribe[LifecycleStateChanged]]
    subscribe.ref shouldBe subscriberProbe.ref
  }

  test("supervisor should handle ComponentStateSubscription message by coordinating with pub sub actor") {
    val sampleHcdHandler: ComponentHandlers[HcdDomainMsg] = mock[ComponentHandlers[HcdDomainMsg]]
    val subscriberProbe                                   = TestProbe[CurrentState]

    val supervisor             = new Supervisor(getContext, hcdInfo, getSampleHcdFactory(sampleHcdHandler))
    val previousSupervisorMode = supervisor.mode
    supervisor.onMessage(ComponentStateSubscription(Subscribe[CurrentState](subscriberProbe.ref)))
    supervisor.mode shouldBe previousSupervisorMode

    val subscribe = pubSubCompStateProbe.expectMsgType[Subscribe[CurrentState]]
    subscribe.ref shouldBe subscriberProbe.ref
  }

}
