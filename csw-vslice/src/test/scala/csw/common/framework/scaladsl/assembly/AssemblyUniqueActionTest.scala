package csw.common.framework.scaladsl.assembly

import akka.typed.testkit.scaladsl.TestProbe
import csw.common.components.assembly.{AssemblyDomainMessages, OperationsMode}
import csw.common.framework.models.AssemblyResponseMode
import csw.common.framework.models.AssemblyResponseMode.{Initialized, Running}
import csw.common.framework.models.InitialAssemblyMsg.Run
import csw.common.framework.models.RunningAssemblyMsg.DomainAssemblyMsg
import csw.common.framework.scaladsl.FrameworkComponentTestSuite
import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class AssemblyUniqueActionTest extends FrameworkComponentTestSuite with MockitoSugar {

  test("assembly component should be able to handle Domain specific messages") {
    val sampleAssemblyHandler = mock[AssemblyHandlers[AssemblyDomainMessages]]

    when(sampleAssemblyHandler.initialize()).thenReturn(Future.unit)

    val supervisorProbe: TestProbe[AssemblyResponseMode] = TestProbe[AssemblyResponseMode]

    Await.result(
      system.systemActorOf[Nothing](
        getSampleAssemblyFactory(sampleAssemblyHandler).behavior(assemblyInfo, supervisorProbe.ref),
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
