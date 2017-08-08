package csw.common.framework.scaladsl.hcd

import akka.typed.testkit.StubbedActorContext
import akka.typed.testkit.scaladsl.TestProbe
import csw.common.components.hcd.HcdDomainMsg
import csw.common.framework.internal.ComponentBehavior
import csw.common.framework.models.IdleMsg.Initialize
import csw.common.framework.models.InitialMsg.Run
import csw.common.framework.models.PreparingToShutdownMsg.ShutdownComplete
import csw.common.framework.models.PubSub.PublisherMsg
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.SupervisorIdleMsg.{Initialized, Running}
import csw.common.framework.models.{ComponentMsg, FromComponentLifecycleMessage, ToComponentLifecycleMessage}
import csw.common.framework.scaladsl.{ComponentHandlers, FrameworkComponentTestSuite}
import csw.param.states.CurrentState
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future

class StubbedHcdLifecycleHooksTest extends FrameworkComponentTestSuite with MockitoSugar {

  def getComponentBehavior(
      hcdHandlers: ComponentHandlers[HcdDomainMsg],
      supervisorProbe: TestProbe[FromComponentLifecycleMessage]
  ): ComponentBehavior[HcdDomainMsg] = {

    val ctx               = new StubbedActorContext[ComponentMsg]("test-component", 100, system)
    val componentBehavior = new ComponentBehavior[HcdDomainMsg](ctx, supervisorProbe.ref, hcdHandlers)

    componentBehavior.onMessage(Initialize)
    supervisorProbe.expectMsgType[Initialized]
    Thread.sleep(100)
    componentBehavior.onMessage(Run)
    supervisorProbe.expectMsgType[Running]
    ctx.selfInbox.receiveMsg() shouldBe Initialize

    componentBehavior
  }

  override protected def afterAll(): Unit = {
    system.terminate()
  }

  test("A running Hcd component should accept Shutdown lifecycle message") {
    val sampleHcdHandler = mock[ComponentHandlers[HcdDomainMsg]]
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)

    val testProbeSupervisor = TestProbe[FromComponentLifecycleMessage]
    val componentBehavior   = getComponentBehavior(sampleHcdHandler, testProbeSupervisor)

    doNothing().when(sampleHcdHandler).onShutdown()

    componentBehavior.onMessage(Lifecycle(ToComponentLifecycleMessage.Shutdown))
    testProbeSupervisor.expectMsg(ShutdownComplete)
    verify(sampleHcdHandler).onShutdown()
  }

  test("A running Hcd component should accept Restart lifecycle message") {
    val sampleHcdHandler = mock[ComponentHandlers[HcdDomainMsg]]
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)

    val testProbeSupervisor = TestProbe[FromComponentLifecycleMessage]
    val componentBehavior   = getComponentBehavior(sampleHcdHandler, testProbeSupervisor)

    componentBehavior.onMessage(Lifecycle(ToComponentLifecycleMessage.Restart))

    verify(sampleHcdHandler).onRestart()
  }

  test("A running Hcd component should accept RunOffline lifecycle message") {
    val sampleHcdHandler = mock[ComponentHandlers[HcdDomainMsg]]
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)
    when(sampleHcdHandler.isOnline).thenReturn(true)

    val testProbeSupervisor = TestProbe[FromComponentLifecycleMessage]
    val componentBehavior   = getComponentBehavior(sampleHcdHandler, testProbeSupervisor)

    componentBehavior.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOffline))
    Thread.sleep(1000)

    verify(sampleHcdHandler).onGoOffline()
  }

  test("A running Hcd component should not accept RunOffline lifecycle message when it is already offline") {
    val sampleHcdHandler = mock[ComponentHandlers[HcdDomainMsg]]
    when(sampleHcdHandler.isOnline).thenReturn(false)
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)

    val testProbeSupervisor = TestProbe[FromComponentLifecycleMessage]
    val componentBehavior   = getComponentBehavior(sampleHcdHandler, testProbeSupervisor)

    componentBehavior.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOffline))
    Thread.sleep(1000)

    verify(sampleHcdHandler, never).onGoOffline()
  }

  test("A running Hcd component should accept RunOnline lifecycle message when it is Offline") {
    val sampleHcdHandler = mock[ComponentHandlers[HcdDomainMsg]]
    when(sampleHcdHandler.isOnline).thenReturn(false)
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)

    val testProbeSupervisor = TestProbe[FromComponentLifecycleMessage]
    val componentBehavior   = getComponentBehavior(sampleHcdHandler, testProbeSupervisor)

    componentBehavior.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOnline))
    Thread.sleep(1000)

    verify(sampleHcdHandler).onGoOnline()
  }

  test("A running Hcd component should not accept RunOnline lifecycle message when it is already Online") {
    val sampleHcdHandler = mock[ComponentHandlers[HcdDomainMsg]]
    when(sampleHcdHandler.isOnline).thenReturn(true)
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)

    val testProbeSupervisor = TestProbe[FromComponentLifecycleMessage]
    val componentBehavior   = getComponentBehavior(sampleHcdHandler, testProbeSupervisor)

    componentBehavior.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOnline))
    Thread.sleep(1000)

    verify(sampleHcdHandler, never).onGoOnline()
  }
}
