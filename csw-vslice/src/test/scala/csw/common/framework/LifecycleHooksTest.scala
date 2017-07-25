package csw.common.framework

import akka.typed.{ActorSystem, Behavior, PostStop, Signal}
import akka.typed.scaladsl.Actor
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.util.Timeout
import csw.common.components.hcd._
import csw.common.framework.models.FromComponentLifecycleMessage.ShutdownComplete
import csw.common.framework.models.HcdResponseMode.{Idle, Initialized, Running}
import csw.common.framework.models.InitialHcdMsg.Run
import csw.common.framework.models.RunningHcdMsg.{DomainHcdMsg, Lifecycle}
import csw.common.framework.models.{HcdMsg, HcdResponseMode, ToComponentLifecycleMessage}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class LifecycleHooksTest extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  implicit val system   = ActorSystem("testHcd", Actor.empty)
  implicit val settings = TestKitSettings(system)
  implicit val timeout  = Timeout(5.seconds)

  def run(testProbeSupervisor: TestProbe[HcdResponseMode], value1: Behavior[Nothing]): Running = {
    Await.result(
      system.systemActorOf[Nothing](value1, "Hcd"),
      5.seconds
    )
    val initialized = testProbeSupervisor.expectMsgType[Initialized]
    initialized.hcdRef ! Run(testProbeSupervisor.ref)
    testProbeSupervisor.expectMsgType[Running]
  }

  override protected def afterAll(): Unit = {
    system.terminate()
  }

  test("A runnning Hcd component should accept Shutdown lifecycle message") {
    val testProbeSupervisor = TestProbe[HcdResponseMode]
    val beh = Actor
      .mutable[HcdMsg](ctx ⇒ {
        new SampleHcd(ctx, testProbeSupervisor.ref) {
          override def onSignal: PartialFunction[Signal, Behavior[HcdMsg]] = {
            case PostStop ⇒ testProbeSupervisor.ref ! Idle; Actor.same
          }
        }
      })
      .narrow
    val running = run(testProbeSupervisor, beh)
    running.hcdRef ! Lifecycle(ToComponentLifecycleMessage.Shutdown)
    testProbeSupervisor.expectMsg(ShutdownComplete)
//    testProbeSupervisor.expectMsg(Idle)
  }

  test("A runnning Hcd component should accept Restart lifecycle message") {
    val testProbeSupervisor = TestProbe[HcdResponseMode]
    val running             = run(testProbeSupervisor, SampleHcdFactory.behaviour(testProbeSupervisor.ref))
    running.hcdRef ! Lifecycle(ToComponentLifecycleMessage.Restart)
    val initialized = testProbeSupervisor.expectMsgType[Initialized]

    initialized.hcdRef ! Run(testProbeSupervisor.ref)
    val running1 = testProbeSupervisor.expectMsgType[Running]

    val testProbeStateRecieiver: TestProbe[DomainResponseMsg] = TestProbe[DomainResponseMsg]
    running1.hcdRef ! DomainHcdMsg(GetCurrentState(testProbeStateRecieiver.ref))
    val response = testProbeStateRecieiver.expectMsgType[HcdDomainResponseMsg]
    response.state shouldBe LifecycleMessageReceived.Restart
  }
}
