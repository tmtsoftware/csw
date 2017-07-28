package csw.common.framework.scaladsl.hcd

import akka.typed.ActorSystem
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.util.Timeout
import csw.common.components.hcd._
import csw.common.framework.models.Component.{DoNotRegister, HcdInfo}
import csw.common.framework.models.FromComponentLifecycleMessage.ShutdownComplete
import csw.common.framework.models.HcdResponseMode.{Initialized, Running}
import csw.common.framework.models.InitialHcdMsg.Run
import csw.common.framework.models.RunningHcdMsg.Lifecycle
import csw.common.framework.models.{HcdMsg, HcdResponseMode, ToComponentLifecycleMessage}
import csw.services.location.models.ConnectionType.AkkaType
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, Future}

class LifecycleHooksTest
    extends FunSuite
    with Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with MockitoSugar {

  implicit val system   = ActorSystem("testHcd", Actor.empty)
  implicit val settings = TestKitSettings(system)
  implicit val timeout  = Timeout(5.seconds)

  class SampleHcdHandlersFactory(sampleHcdHandler: HcdHandlers[HcdDomainMessage])
      extends HcdHandlersFactory[HcdDomainMessage] {
    override def make(ctx: ActorContext[HcdMsg], hcdInfo: HcdInfo): HcdHandlers[HcdDomainMessage] = sampleHcdHandler
  }

  def run(hcdHandlersFactory: HcdHandlersFactory[HcdDomainMessage],
          testProbeSupervisor: TestProbe[HcdResponseMode]): Running = {
    val hcdInfo =
      HcdInfo("SampleHcd",
              "wfos",
              "csw.common.components.assembly.SampleAssembly",
              DoNotRegister,
              Set(AkkaType),
              FiniteDuration(5, "seconds"))

    Await.result(
      system.systemActorOf[Nothing](hcdHandlersFactory.behaviour(hcdInfo, testProbeSupervisor.ref), "Hcd"),
      5.seconds
    )

    val initialized = testProbeSupervisor.expectMsgType[Initialized]
    initialized.hcdRef ! Run
    testProbeSupervisor.expectMsgType[Running]
  }

  override protected def afterAll(): Unit = {
    system.terminate()
  }

  test("A running Hcd component should accept Shutdown lifecycle message") {
    val sampleHcdHandler = mock[HcdHandlers[HcdDomainMessage]]
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)

    val testProbeSupervisor = TestProbe[HcdResponseMode]
    val running             = run(new SampleHcdHandlersFactory(sampleHcdHandler), testProbeSupervisor)

    doNothing().when(sampleHcdHandler).onShutdown()

    running.hcdRef ! Lifecycle(ToComponentLifecycleMessage.Shutdown)
    testProbeSupervisor.expectMsg(ShutdownComplete)
    verify(sampleHcdHandler).onShutdown()
    verify(sampleHcdHandler).stopChildren()
  }

  test("A running Hcd component should accept Restart lifecycle message") {
    val sampleHcdHandler = mock[HcdHandlers[HcdDomainMessage]]
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)

    val testProbeSupervisor = TestProbe[HcdResponseMode]
    val running             = run(new SampleHcdHandlersFactory(sampleHcdHandler), testProbeSupervisor)

    running.hcdRef ! Lifecycle(ToComponentLifecycleMessage.Restart)
    Thread.sleep(1000)

    verify(sampleHcdHandler).onRestart()
  }

  test("A running Hcd component should accept RunOffline lifecycle message") {
    val sampleHcdHandler = mock[HcdHandlers[HcdDomainMessage]]
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)
    when(sampleHcdHandler.isOnline).thenReturn(true)

    val testProbeSupervisor = TestProbe[HcdResponseMode]
    val running             = run(new SampleHcdHandlersFactory(sampleHcdHandler), testProbeSupervisor)

    running.hcdRef ! Lifecycle(ToComponentLifecycleMessage.GoOffline)
    Thread.sleep(1000)

    verify(sampleHcdHandler).onGoOffline()
  }

  test("A running Hcd component should not accept RunOffline lifecycle message when it is already offline") {
    val sampleHcdHandler = mock[HcdHandlers[HcdDomainMessage]]
    when(sampleHcdHandler.isOnline).thenReturn(false)
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)

    val testProbeSupervisor = TestProbe[HcdResponseMode]
    val running             = run(new SampleHcdHandlersFactory(sampleHcdHandler), testProbeSupervisor)

    running.hcdRef ! Lifecycle(ToComponentLifecycleMessage.GoOffline)
    Thread.sleep(1000)

    verify(sampleHcdHandler, never).onGoOffline()
  }

  test("A running Hcd component should accept RunOnline lifecycle message when it is Offline") {
    val sampleHcdHandler = mock[HcdHandlers[HcdDomainMessage]]
    when(sampleHcdHandler.isOnline).thenReturn(false)
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)

    val testProbeSupervisor = TestProbe[HcdResponseMode]
    val running             = run(new SampleHcdHandlersFactory(sampleHcdHandler), testProbeSupervisor)

    running.hcdRef ! Lifecycle(ToComponentLifecycleMessage.GoOnline)
    Thread.sleep(1000)

    verify(sampleHcdHandler).onGoOnline()
  }

  test("A running Hcd component should not accept RunOnline lifecycle message when it is already Online") {
    val sampleHcdHandler = mock[HcdHandlers[HcdDomainMessage]]
    when(sampleHcdHandler.isOnline).thenReturn(true)
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)

    val testProbeSupervisor = TestProbe[HcdResponseMode]
    val running             = run(new SampleHcdHandlersFactory(sampleHcdHandler), testProbeSupervisor)

    running.hcdRef ! Lifecycle(ToComponentLifecycleMessage.GoOnline)
    Thread.sleep(1000)

    verify(sampleHcdHandler, never).onGoOnline()
  }
}
