package csw.common.framework.scaladsl.assembly

import akka.typed.testkit.scaladsl.TestProbe
import csw.common.components.assembly.AssemblyDomainMsg
import csw.common.framework.models.FromComponentLifecycleMessage
import csw.common.framework.models.InitialMsg.Run
import csw.common.framework.models.SupervisorIdleMsg.{InitializeFailure, Initialized, Running}
import csw.common.framework.scaladsl.FrameworkComponentTestSuite
import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class AssemblyBehaviorTest extends FrameworkComponentTestSuite with MockitoSugar {

  test("Assembly actor sends Initialized and Running message to Supervisor") {
    val sampleAssemblyHandler = mock[AssemblyHandlers[AssemblyDomainMsg]]

    when(sampleAssemblyHandler.initialize()).thenReturn(Future.unit)

    val supervisorProbe = TestProbe[FromComponentLifecycleMessage]

    val assemblyRef =
      Await.result(
        system.systemActorOf[Nothing](
          getSampleAssemblyFactory(sampleAssemblyHandler).behavior(assemblyInfo, supervisorProbe.ref),
          "assembly"
        ),
        5.seconds
      )

    val initialized = supervisorProbe.expectMsgType[Initialized]
    initialized.componentRef shouldBe assemblyRef

    initialized.componentRef ! Run

    val running = supervisorProbe.expectMsgType[Running]
    verify(sampleAssemblyHandler).onRun()
    verify(sampleAssemblyHandler).isOnline_=(true)

    running.componentRef shouldBe assemblyRef
  }

  test("A Assembly component should send InitializationFailure message if it fails in initialization") {
    val sampleAssemblyHandler = mock[AssemblyHandlers[AssemblyDomainMsg]]
    val exceptionReason       = "test Exception"
    when(sampleAssemblyHandler.initialize()).thenThrow(new RuntimeException(exceptionReason))

    val supervisorProbe: TestProbe[FromComponentLifecycleMessage] = TestProbe[FromComponentLifecycleMessage]

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
