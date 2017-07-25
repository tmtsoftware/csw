package csw.common.framework

import akka.typed.scaladsl.Actor
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.common.components.hcd.SampleHcdFactory
import csw.common.framework.models.HcdResponseMode.{Initialized, Running}
import csw.common.framework.models.InitialHcdMsg.Run
import csw.common.framework.models.RunningHcdMsg.Lifecycle
import csw.common.framework.models.{HcdResponseMode, ShutdownComplete, ToComponentLifecycleMessage}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class LifecycleHooksTest extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  implicit val system   = ActorSystem("testHcd", Actor.empty)
  implicit val settings = TestKitSettings(system)
  implicit val timeout  = Timeout(5.seconds)

  val testProbe: TestProbe[HcdResponseMode] = TestProbe[HcdResponseMode]

  val SampleHcdFactory = new SampleHcdFactory

  val hcdRef: ActorRef[Nothing] =
    Await.result(system.systemActorOf[Nothing](SampleHcdFactory.behaviour(testProbe.ref), "Hcd"), 5.seconds)

  val initialized: Initialized = testProbe.expectMsgType[Initialized]
  initialized.hcdRef ! Run(testProbe.ref)

  val running: Running = testProbe.expectMsgType[Running]

  override protected def afterAll(): Unit = {
    system.terminate()
  }

  test("A runnning Hcd component should be should accept Shutdown lifecycle message") {
    running.hcdRef ! Lifecycle(ToComponentLifecycleMessage.Shutdown)
    testProbe.expectMsg[HcdResponseMode](ShutdownComplete)
  }

  test("A runnning Hcd component should be should accept Restart lifecycle message") {
    running.hcdRef ! Lifecycle(ToComponentLifecycleMessage.Restart)
    testProbe.expectMsgType[Initialized]
  }
}
