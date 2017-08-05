package csw.common.framework.internal.supervisor

import akka.typed.{Behavior, Props}
import akka.typed.scaladsl.ActorContext
import akka.typed.testkit.scaladsl.TestProbe
import csw.common.components.hcd.HcdDomainMsg
import csw.common.framework.internal.PubSubActor
import csw.common.framework.models.{ComponentMsg, IdleMsg, LifecycleStateChanged, PubSub, SupervisorMsg}
import csw.common.framework.scaladsl.{ComponentBehaviorFactory, FrameworkComponentTestSuite}
import csw.param.StateVariable.CurrentState
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuiteLike, Matchers}

class SupervisorTest extends FrameworkComponentTestSuite with FunSuiteLike with Matchers with MockitoSugar {

  ignore("effects") {

    val testComponent            = TestProbe[ComponentMsg]
    val testPubSubCompState      = TestProbe[PubSub[CurrentState]]
    val testPubSubLifecycle      = TestProbe[PubSub[LifecycleStateChanged]]
    val context                  = mock[ActorContext[SupervisorMsg]]
    val componentBehaviorFactory = mock[ComponentBehaviorFactory[HcdDomainMsg]]

    when(componentBehaviorFactory.behavior(hcdInfo, context.self, testPubSubCompState.ref)).thenCallRealMethod()

    when(context.spawnAnonymous(PubSubActor.behavior[CurrentState], Props.empty)).thenReturn(testPubSubCompState.ref)
    when(context.spawnAnonymous(PubSubActor.behavior[LifecycleStateChanged], Props.empty))
      .thenReturn(testPubSubLifecycle.ref)

    when(context.spawnAnonymous[Nothing](notNull[Behavior[Nothing]], ArgumentMatchers.eq(Props.empty)))
      .thenReturn(testComponent.ref)

    new Supervisor(context, hcdInfo, componentBehaviorFactory)

    val i = testComponent.expectMsgType[IdleMsg.Initialize.type]
  }
}
