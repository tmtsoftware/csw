package csw.common.framework

import akka.typed.ActorSystem
import akka.typed.scaladsl.Actor
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.util.Timeout
import csw.common.components.TestHcd
import csw.common.framework.HcdComponentLifecycleMessage.{Initialized, Running}
import csw.common.framework.InitialHcdMsg.Run
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

sealed trait TestHcdMessage extends DomainMsg

class HcdFrameworkTest extends FunSuite with Matchers {

  test("hcd") {
    implicit val system   = ActorSystem("testHcd", Actor.empty)
    implicit val settings = TestKitSettings(system)
    implicit val timeout  = Timeout(5.seconds)

    val testProbe: TestProbe[HcdComponentLifecycleMessage] = TestProbe[HcdComponentLifecycleMessage]

    val hcdRef =
      Await.result(system.systemActorOf[Nothing](TestHcd.behaviour(testProbe.ref), "Hcd"), 5.seconds)

    val initialized = testProbe.expectMsgType[Initialized]
    initialized.hcdRef shouldBe hcdRef

    initialized.hcdRef ! Run(testProbe.ref)
    val running = testProbe.expectMsgType[Running]
    running.hcdRef shouldBe hcdRef
  }
}
