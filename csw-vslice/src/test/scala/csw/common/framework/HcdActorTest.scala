package csw.common.framework

import akka.typed.ActorSystem
import akka.typed.scaladsl.Actor
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.util.Timeout
import csw.common.components.hcd.SampleHcdFactory
import csw.common.framework.models.HcdResponseMode
import csw.common.framework.models.HcdResponseMode.{Initialized, Running}
import csw.common.framework.models.InitialHcdMsg.Run
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class HcdActorTest extends FunSuite with Matchers with BeforeAndAfterAll {

  implicit val system   = ActorSystem("testHcd", Actor.empty)
  implicit val settings = TestKitSettings(system)
  implicit val timeout  = Timeout(5.seconds)

  override protected def afterAll() = {
    system.terminate()
  }

  test("hcd component should send initialize and running message to supervisor") {
    val testProbe: TestProbe[HcdResponseMode] = TestProbe[HcdResponseMode]

    val hcdRef =
      Await.result(system.systemActorOf[Nothing]((new SampleHcdFactory).behaviour(testProbe.ref), "Hcd"), 5.seconds)

    val initialized = testProbe.expectMsgType[Initialized]
    initialized.hcdRef shouldBe hcdRef

    initialized.hcdRef ! Run(testProbe.ref)
    val running = testProbe.expectMsgType[Running]
    running.hcdRef shouldBe hcdRef
  }
}
