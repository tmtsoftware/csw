package csw.common.framework

import akka.typed.ActorSystem
import akka.typed.scaladsl.Actor
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.util.Timeout
import csw.common.components.hcd._
import csw.common.framework.models.FromComponentLifecycleMessage.ShutdownComplete
import csw.common.framework.models.HcdResponseMode.{Initialized, Running}
import csw.common.framework.models.InitialHcdMsg.Run
import csw.common.framework.models.RunningHcdMsg.{DomainHcdMsg, Lifecycle}
import csw.common.framework.models.{HcdResponseMode, ToComponentLifecycleMessage}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class LifecycleHooksTest extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  implicit val system   = ActorSystem("testHcd", Actor.empty)
  implicit val settings = TestKitSettings(system)
  implicit val timeout  = Timeout(5.seconds)

  def run(testProbeSupervisor: TestProbe[HcdResponseMode]): Running = {
    Await.result(
      system.systemActorOf[Nothing](SampleHcdFactory.behaviour(testProbeSupervisor.ref), "Hcd"),
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
    val testProbeSupervisor = TestProbe[HcdResponseMode]
    val running             = run(testProbeSupervisor)
    running.hcdRef ! Lifecycle(ToComponentLifecycleMessage.Shutdown)
    testProbeSupervisor.expectMsg(ShutdownComplete)
  }

  test("A running Hcd component should accept Restart lifecycle message") {
    val testProbeSupervisor = TestProbe[HcdResponseMode]
    val running             = run(testProbeSupervisor)
    running.hcdRef ! Lifecycle(ToComponentLifecycleMessage.Restart)
    val running1 = testProbeSupervisor.expectMsgType[Running]

    val testProbeStateReceiver: TestProbe[DomainResponseMsg] = TestProbe[DomainResponseMsg]
    running1.hcdRef ! DomainHcdMsg(GetCurrentState(testProbeStateReceiver.ref))
    val response = testProbeStateReceiver.expectMsgType[HcdDomainResponseMsg]
    response.state shouldBe null
  }

  test("A running Hcd component should accept RunOffline lifecycle message") {
    val testProbeSupervisor = TestProbe[HcdResponseMode]
    val running             = run(testProbeSupervisor)
    running.hcdRef ! Lifecycle(ToComponentLifecycleMessage.GoOffline)

    val testProbeStateReceiver: TestProbe[DomainResponseMsg] = TestProbe[DomainResponseMsg]
    running.hcdRef ! DomainHcdMsg(GetCurrentState(testProbeStateReceiver.ref))
    val response = testProbeStateReceiver.expectMsgType[HcdDomainResponseMsg]
    response.state shouldBe LifecycleMessageReceived.RunOffline
  }

  test("A running Hcd component should accept RunOnline lifecycle message when it is Offline") {
    val testProbeSupervisor = TestProbe[HcdResponseMode]
    val running             = run(testProbeSupervisor)
    running.hcdRef ! Lifecycle(ToComponentLifecycleMessage.GoOffline)
    running.hcdRef ! Lifecycle(ToComponentLifecycleMessage.GoOnline)

    val testProbeStateReceiver: TestProbe[DomainResponseMsg] = TestProbe[DomainResponseMsg]
    running.hcdRef ! DomainHcdMsg(GetCurrentState(testProbeStateReceiver.ref))
    val response = testProbeStateReceiver.expectMsgType[HcdDomainResponseMsg]
    response.state shouldBe LifecycleMessageReceived.RunOnline
  }

  test("A running Hcd component should not accept RunOnline lifecycle message when it is already Online") {
    val testProbeSupervisor = TestProbe[HcdResponseMode]
    val running             = run(testProbeSupervisor)
    running.hcdRef ! Lifecycle(ToComponentLifecycleMessage.GoOnline)

    val testProbeStateReceiver: TestProbe[DomainResponseMsg] = TestProbe[DomainResponseMsg]
    running.hcdRef ! DomainHcdMsg(GetCurrentState(testProbeStateReceiver.ref))
    val response = testProbeStateReceiver.expectMsgType[HcdDomainResponseMsg]
    response.state should not be LifecycleMessageReceived.RunOnline
  }
}
