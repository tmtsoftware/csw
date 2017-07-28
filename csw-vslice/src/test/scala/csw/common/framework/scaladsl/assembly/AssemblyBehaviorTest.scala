package csw.common.framework.scaladsl.assembly

import akka.typed.ActorSystem
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.util.Timeout
import csw.common.components.assembly.AssemblyDomainMessages
import csw.common.framework.models.AssemblyResponseMode.{Initialized, Running}
import csw.common.framework.models.Component.{AssemblyInfo, DoNotRegister}
import csw.common.framework.models.InitialAssemblyMsg.Run
import csw.common.framework.models.{AssemblyMsg, AssemblyResponseMode}
import csw.services.location.models.ConnectionType.AkkaType
import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class AssemblyBehaviorTest extends FunSuite with Matchers with BeforeAndAfterAll with MockitoSugar {

  implicit val system   = ActorSystem("actor-system", Actor.empty)
  implicit val settings = TestKitSettings(system)
  implicit val timeout  = Timeout(5.seconds)

  override def afterAll(): Unit = {
    system.terminate()
  }

  def getSampleHcdFactory(
      assemblyHandlers: AssemblyHandlers[AssemblyDomainMessages]
  ): AssemblyHandlersFactory[AssemblyDomainMessages] =
    new AssemblyHandlersFactory[AssemblyDomainMessages] {
      override def make(ctx: ActorContext[AssemblyMsg],
                        assemblyInfo: AssemblyInfo): AssemblyHandlers[AssemblyDomainMessages] = assemblyHandlers
    }

  test("Assembly actor sends Initialized and Running message to Supervisor") {
    val sampleAssemblyHandler = mock[AssemblyHandlers[AssemblyDomainMessages]]

    when(sampleAssemblyHandler.initialize()).thenReturn(Future.unit)

    val supervisorProbe = TestProbe[AssemblyResponseMode]

    val assemblyInfo = AssemblyInfo("trombone",
                                    "wfos",
                                    "csw.common.components.assembly.SampleAssembly",
                                    DoNotRegister,
                                    Set(AkkaType),
                                    Set.empty)

    val assemblyRef =
      Await.result(
        system.systemActorOf[Nothing](
          getSampleHcdFactory(sampleAssemblyHandler).behaviour(assemblyInfo, supervisorProbe.ref),
          "assembly"
        ),
        5.seconds
      )

    val initialized = supervisorProbe.expectMsgType[Initialized]
    initialized.assemblyRef shouldBe assemblyRef

    initialized.assemblyRef ! Run

    val running = supervisorProbe.expectMsgType[Running]
    verify(sampleAssemblyHandler).onRun()
    verify(sampleAssemblyHandler).isOnline_=(true)

    running.assemblyRef shouldBe assemblyRef
  }
}
