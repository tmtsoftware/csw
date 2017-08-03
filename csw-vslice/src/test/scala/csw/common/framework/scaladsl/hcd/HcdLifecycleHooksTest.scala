package csw.common.framework.scaladsl.hcd

import akka.typed.testkit.scaladsl.TestProbe
import csw.common.components.hcd.HcdDomainMsg
import csw.common.framework.models.InitialMsg.Run
import csw.common.framework.models.PreparingToShutdownMsg.ShutdownComplete
import csw.common.framework.models.PubSub.PublisherMsg
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.SupervisorIdleMsg.{Initialized, Running}
import csw.common.framework.models.{FromComponentLifecycleMessage, ToComponentLifecycleMessage}
import csw.common.framework.scaladsl.FrameworkComponentTestSuite
import csw.param.StateVariable.CurrentState
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class HcdLifecycleHooksTest extends FrameworkComponentTestSuite with MockitoSugar {

  def run(hcdHandlersFactory: HcdBehaviorFactory[HcdDomainMsg],
          testProbeSupervisor: TestProbe[FromComponentLifecycleMessage]): Running = {

    val publisherProbe = TestProbe[PublisherMsg[CurrentState]]

    Await.result(
      system.systemActorOf[Nothing](hcdHandlersFactory.behavior(hcdInfo, testProbeSupervisor.ref, publisherProbe.ref),
                                    "Hcd"),
      5.seconds
    )

    val initialized = testProbeSupervisor.expectMsgType[Initialized]
    initialized.componentRef ! Run
    testProbeSupervisor.expectMsgType[Running]
  }

  override protected def afterAll(): Unit = {
    system.terminate()
  }

  test("A running Hcd component should accept Shutdown lifecycle message") {
    val sampleHcdHandler = mock[HcdHandlers[HcdDomainMsg]]
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)

    val testProbeSupervisor = TestProbe[FromComponentLifecycleMessage]
    val running             = run(getSampleHcdFactory(sampleHcdHandler), testProbeSupervisor)

    doNothing().when(sampleHcdHandler).onShutdown()

    running.componentRef ! Lifecycle(ToComponentLifecycleMessage.Shutdown)
    testProbeSupervisor.expectMsg(ShutdownComplete)
    verify(sampleHcdHandler).onShutdown()
  }

  test("A running Hcd component should accept Restart lifecycle message") {
    val sampleHcdHandler = mock[HcdHandlers[HcdDomainMsg]]
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)

    val testProbeSupervisor = TestProbe[FromComponentLifecycleMessage]
    val running             = run(getSampleHcdFactory(sampleHcdHandler), testProbeSupervisor)

    running.componentRef ! Lifecycle(ToComponentLifecycleMessage.Restart)
    Thread.sleep(1000)

    verify(sampleHcdHandler).onRestart()
  }

  test("A running Hcd component should accept RunOffline lifecycle message") {
    val sampleHcdHandler = mock[HcdHandlers[HcdDomainMsg]]
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)
    when(sampleHcdHandler.isOnline).thenReturn(true)

    val testProbeSupervisor = TestProbe[FromComponentLifecycleMessage]
    val running             = run(getSampleHcdFactory(sampleHcdHandler), testProbeSupervisor)

    running.componentRef ! Lifecycle(ToComponentLifecycleMessage.GoOffline)
    Thread.sleep(1000)

    verify(sampleHcdHandler).onGoOffline()
  }

  test("A running Hcd component should not accept RunOffline lifecycle message when it is already offline") {
    val sampleHcdHandler = mock[HcdHandlers[HcdDomainMsg]]
    when(sampleHcdHandler.isOnline).thenReturn(false)
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)

    val testProbeSupervisor = TestProbe[FromComponentLifecycleMessage]
    val running             = run(getSampleHcdFactory(sampleHcdHandler), testProbeSupervisor)

    running.componentRef ! Lifecycle(ToComponentLifecycleMessage.GoOffline)
    Thread.sleep(1000)

    verify(sampleHcdHandler, never).onGoOffline()
  }

  test("A running Hcd component should accept RunOnline lifecycle message when it is Offline") {
    val sampleHcdHandler = mock[HcdHandlers[HcdDomainMsg]]
    when(sampleHcdHandler.isOnline).thenReturn(false)
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)

    val testProbeSupervisor = TestProbe[FromComponentLifecycleMessage]
    val running             = run(getSampleHcdFactory(sampleHcdHandler), testProbeSupervisor)

    running.componentRef ! Lifecycle(ToComponentLifecycleMessage.GoOnline)
    Thread.sleep(1000)

    verify(sampleHcdHandler).onGoOnline()
  }

  test("A running Hcd component should not accept RunOnline lifecycle message when it is already Online") {
    val sampleHcdHandler = mock[HcdHandlers[HcdDomainMsg]]
    when(sampleHcdHandler.isOnline).thenReturn(true)
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)

    val testProbeSupervisor = TestProbe[FromComponentLifecycleMessage]
    val running             = run(getSampleHcdFactory(sampleHcdHandler), testProbeSupervisor)

    running.componentRef ! Lifecycle(ToComponentLifecycleMessage.GoOnline)
    Thread.sleep(1000)

    verify(sampleHcdHandler, never).onGoOnline()
  }
}
