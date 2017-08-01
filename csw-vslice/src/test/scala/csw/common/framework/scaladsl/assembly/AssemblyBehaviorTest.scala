package csw.common.framework.scaladsl.assembly

import akka.typed.testkit.scaladsl.TestProbe
import csw.common.components.assembly.AssemblyDomainMessages
import csw.common.framework.models.AssemblyResponseMode
import csw.common.framework.models.AssemblyResponseMode.{Initialized, Running}
import csw.common.framework.models.FromComponentLifecycleMessage.InitializeFailure
import csw.common.framework.models.InitialAssemblyMsg.Run
import csw.common.framework.scaladsl.FrameworkComponentTestSuite
import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class AssemblyBehaviorTest extends FrameworkComponentTestSuite with MockitoSugar {

  test("Assembly actor sends Initialized and Running message to Supervisor") {
    val sampleAssemblyHandler = mock[AssemblyHandlers[AssemblyDomainMessages]]

    when(sampleAssemblyHandler.initialize()).thenReturn(Future.unit)

    val supervisorProbe = TestProbe[AssemblyResponseMode]

    val assemblyRef =
      Await.result(
        system.systemActorOf[Nothing](
          getSampleAssemblyFactory(sampleAssemblyHandler).behavior(assemblyInfo, supervisorProbe.ref),
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

  test("A Assembly component should send InitializationFailure message if it fails in initialization") {
    val sampleAssemblyHandler = mock[AssemblyHandlers[AssemblyDomainMessages]]
    val exceptionReason       = "test Exception"
    when(sampleAssemblyHandler.initialize()).thenThrow(new RuntimeException(exceptionReason))

    val supervisorProbe: TestProbe[AssemblyResponseMode] = TestProbe[AssemblyResponseMode]

    Await.result(
      system.systemActorOf[Nothing](
        getSampleAssemblyFactory(sampleAssemblyHandler).behavior(assemblyInfo, supervisorProbe.ref),
        "sampleAssembly"
      ),
      5.seconds
    )

    val initializationFailure = supervisorProbe.expectMsgType[InitializeFailure]
    initializationFailure.reason shouldBe exceptionReason
  }

}
