package csw.common.framework.internal.supervisor

import akka.typed.scaladsl.ActorContext
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{Behavior, Props}
import csw.common.components.hcd.HcdDomainMsg
import csw.common.framework.internal.PubSubActor
import csw.common.framework.models.InitialMsg.Run
import csw.common.framework.models.SupervisorIdleMsg.Initialized
import csw.common.framework.models._
import csw.common.framework.scaladsl.{ComponentHandlers, FrameworkComponentTestSuite}
import csw.param.StateVariable.CurrentState
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuiteLike, Matchers}

class SupervisorTest2 extends FrameworkComponentTestSuite with FunSuiteLike with Matchers with MockitoSugar {

  ignore("effects") {

    val testComponent       = TestProbe[ComponentMsg]
    val testPubSubCompState = TestProbe[PubSub[CurrentState]]
    val testPubSubLifecycle = TestProbe[PubSub[LifecycleStateChanged]]
    val testSupervisor      = TestProbe[SupervisorMsg]
    val context             = mock[ActorContext[SupervisorMsg]]
    val sampleHcdHandler    = mock[ComponentHandlers[HcdDomainMsg]]

    when(context.spawnAnonymous(PubSubActor.behavior[CurrentState])).thenReturn(testPubSubCompState.ref)
    when(context.spawnAnonymous(PubSubActor.behavior[LifecycleStateChanged]))
      .thenReturn(testPubSubLifecycle.ref)

    when(context.spawnAnonymous[Nothing](any[Behavior[Nothing]], any[Props]())).thenReturn(testComponent.ref)

    when(context.self).thenReturn(testSupervisor.ref)
    doNothing().when(context).watch(any())

    new Supervisor(context, hcdInfo, getSampleHcdFactory(sampleHcdHandler))

    context.self ! Initialized(testComponent.ref)

    val initializedComponent = testSupervisor.expectMsgType[Initialized]
    initializedComponent.componentRef shouldBe testComponent.ref

    testComponent.expectMsgType[Run.type]
  }

}
