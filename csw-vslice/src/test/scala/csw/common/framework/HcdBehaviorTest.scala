package csw.common.framework

import akka.typed.ActorSystem
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.util.Timeout
import csw.common.components.hcd.HcdDomainMessage
import csw.common.framework.models.HcdResponseMode.{Initialized, Running}
import csw.common.framework.models.InitialHcdMsg.Run
import csw.common.framework.models.{HcdMsg, HcdResponseMode}
import csw.common.framework.scaladsl.hcd.{HcdHandlers, HcdHandlersFactory}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt

class HcdBehaviorTest extends FunSuite with Matchers with BeforeAndAfterAll with MockitoSugar {

  implicit val system   = ActorSystem("testHcd", Actor.empty)
  implicit val settings = TestKitSettings(system)
  implicit val timeout  = Timeout(5.seconds)

  override protected def afterAll(): Unit = {
    system.terminate()
  }

  test("hcd component should send initialize and running message to supervisor") {
    val sampleHcdHandler = mock[HcdHandlers[HcdDomainMessage]]
    val sampleHcdHandlersFactory = new HcdHandlersFactory[HcdDomainMessage] {
      override def make(ctx: ActorContext[HcdMsg]): HcdHandlers[HcdDomainMessage] = sampleHcdHandler
    }
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)
    val testProbe: TestProbe[HcdResponseMode] = TestProbe[HcdResponseMode]

    val hcdRef =
      Await.result(system.systemActorOf[Nothing](sampleHcdHandlersFactory.behaviour(testProbe.ref), "sampleHcd"),
                   5.seconds)

    val initialized = testProbe.expectMsgType[Initialized]
    initialized.hcdRef shouldBe hcdRef

    initialized.hcdRef ! Run

    val running = testProbe.expectMsgType[Running]
    running.hcdRef shouldBe hcdRef
  }
}
