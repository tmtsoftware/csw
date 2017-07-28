package csw.common.framework.scaladsl.hcd

import akka.typed.ActorSystem
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.util.Timeout
import csw.common.components.hcd.HcdDomainMessage
import csw.common.framework.models.Component.{DoNotRegister, HcdInfo}
import csw.common.framework.models.HcdResponseMode.{Initialized, Running}
import csw.common.framework.models.InitialHcdMsg.Run
import csw.common.framework.models.{HcdMsg, HcdResponseMode}
import csw.services.location.models.ConnectionType.AkkaType
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, Future}

class HcdBehaviorTest extends FunSuite with Matchers with BeforeAndAfterAll with MockitoSugar {

  implicit val system   = ActorSystem("testHcd", Actor.empty)
  implicit val settings = TestKitSettings(system)
  implicit val timeout  = Timeout(5.seconds)

  override protected def afterAll(): Unit = {
    system.terminate()
  }

  def getSampleHcdFactory(hcdHandlers: HcdHandlers[HcdDomainMessage]): HcdHandlersFactory[HcdDomainMessage] =
    new HcdHandlersFactory[HcdDomainMessage] {
      override def make(ctx: ActorContext[HcdMsg], hcdInfo: HcdInfo): HcdHandlers[HcdDomainMessage] = hcdHandlers
    }

  test("hcd component should send initialize and running message to supervisor") {
    val sampleHcdHandler = mock[HcdHandlers[HcdDomainMessage]]

    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)

    val supervisorProbe: TestProbe[HcdResponseMode] = TestProbe[HcdResponseMode]

    val hcdInfo =
      HcdInfo("SampleHcd",
              "wfos",
              "csw.common.components.assembly.SampleAssembly",
              DoNotRegister,
              Set(AkkaType),
              FiniteDuration(5, "seconds"))

    val hcdRef =
      Await.result(
        system.systemActorOf[Nothing](getSampleHcdFactory(sampleHcdHandler).behaviour(hcdInfo, supervisorProbe.ref),
                                      "sampleHcd"),
        5.seconds
      )

    val initialized = supervisorProbe.expectMsgType[Initialized]

    verify(sampleHcdHandler).initialize()
    initialized.hcdRef shouldBe hcdRef

    initialized.hcdRef ! Run

    val running = supervisorProbe.expectMsgType[Running]

    verify(sampleHcdHandler).onRun()
    verify(sampleHcdHandler).isOnline_=(true)

    running.hcdRef shouldBe hcdRef
  }
}
