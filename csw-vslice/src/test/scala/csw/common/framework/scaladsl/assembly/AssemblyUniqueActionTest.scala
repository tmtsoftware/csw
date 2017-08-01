package csw.common.framework.scaladsl.assembly

import akka.typed.ActorSystem
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.util.Timeout
import csw.common.components.assembly.{AssemblyDomainMessages, OperationsMode}
import csw.common.framework.models.AssemblyResponseMode.{Initialized, Running}
import csw.common.framework.models.Component.{AssemblyInfo, DoNotRegister}
import csw.common.framework.models.InitialAssemblyMsg.Run
import csw.common.framework.models.RunningAssemblyMsg.DomainAssemblyMsg
import csw.common.framework.models.{AssemblyMsg, AssemblyResponseMode}
import csw.services.location.models.ConnectionType.AkkaType
import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class AssemblyUniqueActionTest extends FunSuite with Matchers with BeforeAndAfterAll with MockitoSugar {

  implicit val system   = ActorSystem("testHcd", Actor.empty)
  implicit val settings = TestKitSettings(system)
  implicit val timeout  = Timeout(5.seconds)

  override protected def afterAll(): Unit = {
    system.terminate()
  }

  def getSampleAssemblyFactory(
      assemblyHandlers: AssemblyHandlers[AssemblyDomainMessages]
  ): AssemblyHandlersFactory[AssemblyDomainMessages] =
    new AssemblyHandlersFactory[AssemblyDomainMessages] {
      override def make(ctx: ActorContext[AssemblyMsg],
                        assemblyInfo: AssemblyInfo): AssemblyHandlers[AssemblyDomainMessages] = assemblyHandlers
    }

  test("assembly component should be able to handle Domain specific messages") {
    val sampleAssemblyHandler = mock[AssemblyHandlers[AssemblyDomainMessages]]

    when(sampleAssemblyHandler.initialize()).thenReturn(Future.unit)

    val supervisorProbe: TestProbe[AssemblyResponseMode] = TestProbe[AssemblyResponseMode]

    val assemblyInfo = AssemblyInfo("trombone",
                                    "wfos",
                                    "csw.common.components.assembly.SampleAssembly",
                                    DoNotRegister,
                                    Set(AkkaType),
                                    Set.empty)

    Await.result(
      system.systemActorOf[Nothing](
        getSampleAssemblyFactory(sampleAssemblyHandler).behaviour(assemblyInfo, supervisorProbe.ref),
        "sampleAssembly"
      ),
      5.seconds
    )

    val initialized = supervisorProbe.expectMsgType[Initialized]
    initialized.assemblyRef ! Run

    val running = supervisorProbe.expectMsgType[Running]
    running.assemblyRef ! DomainAssemblyMsg(OperationsMode)

    Thread.sleep(1000)
    verify(sampleAssemblyHandler).onDomainMsg(OperationsMode)
  }
}
