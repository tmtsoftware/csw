package csw.common.framework.scaladsl.hcd

import akka.typed.ActorSystem
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.util.Timeout
import csw.common.components.assembly.AssemblyDomainMessages
import csw.common.components.hcd.{AxisStatistics, HcdDomainMessage}
import csw.common.framework.models.Component.{AssemblyInfo, DoNotRegister, HcdInfo}
import csw.common.framework.models.HcdResponseMode.{Initialized, Running}
import csw.common.framework.models.InitialHcdMsg.Run
import csw.common.framework.models.RunningHcdMsg.DomainHcdMsg
import csw.common.framework.models.{AssemblyMsg, HcdMsg, HcdResponseMode}
import csw.common.framework.scaladsl.assembly.{AssemblyHandlers, AssemblyHandlersFactory}
import csw.services.location.models.ConnectionType.AkkaType
import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, Future}

class HcdUniqueActionTest extends FunSuite with Matchers with BeforeAndAfterAll with MockitoSugar {

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

  def getSampleAssemblyFactory(
      assemblyHandlers: AssemblyHandlers[AssemblyDomainMessages]
  ): AssemblyHandlersFactory[AssemblyDomainMessages] =
    new AssemblyHandlersFactory[AssemblyDomainMessages] {
      override def make(ctx: ActorContext[AssemblyMsg],
                        assemblyInfo: AssemblyInfo): AssemblyHandlers[AssemblyDomainMessages] = assemblyHandlers
    }

  test("hcd component should be able to handle Domain specific messages") {
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

    Await.result(
      system.systemActorOf[Nothing](getSampleHcdFactory(sampleHcdHandler).behavior(hcdInfo, supervisorProbe.ref),
                                    "sampleHcd"),
      5.seconds
    )

    val initialized = supervisorProbe.expectMsgType[Initialized]
    initialized.hcdRef ! Run

    val running        = supervisorProbe.expectMsgType[Running]
    val axisStatistics = AxisStatistics(1)
    running.hcdRef ! DomainHcdMsg(axisStatistics)

    Thread.sleep(1000)
    verify(sampleHcdHandler).onDomainMsg(AxisStatistics(1))
  }
}
