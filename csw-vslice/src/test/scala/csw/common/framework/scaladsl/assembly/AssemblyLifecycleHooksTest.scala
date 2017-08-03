package csw.common.framework.scaladsl.assembly

import akka.typed.testkit.scaladsl.TestProbe
import csw.common.components.assembly.AssemblyDomainMsg
import csw.common.framework.models.InitialMsg.Run
import csw.common.framework.models.PreparingToShutdownMsg.ShutdownComplete
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.SupervisorIdleMsg.{Initialized, Running}
import csw.common.framework.models.{FromComponentLifecycleMessage, ToComponentLifecycleMessage}
import csw.common.framework.scaladsl.FrameworkComponentTestSuite
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class AssemblyLifecycleHooksTest extends FrameworkComponentTestSuite with MockitoSugar {

  def run(assemblyHandlersFactory: AssemblyBehaviorFactory[AssemblyDomainMsg],
          supervisorProbe: TestProbe[FromComponentLifecycleMessage]): Running = {

    Await.result(
      system.systemActorOf[Nothing](assemblyHandlersFactory.behavior(assemblyInfo, supervisorProbe.ref), "Assembly"),
      5.seconds
    )

    val initialized = supervisorProbe.expectMsgType[Initialized]
    initialized.componentRef ! Run
    supervisorProbe.expectMsgType[Running]
  }

  override protected def afterAll(): Unit = {
    system.terminate()
  }

  test("A running Assembly component should accept Shutdown lifecycle message") {
    val sampleAssemblyHandler = mock[AssemblyHandlers[AssemblyDomainMsg]]
    when(sampleAssemblyHandler.initialize()).thenReturn(Future.unit)

    val supervisorProbe = TestProbe[FromComponentLifecycleMessage]
    val running         = run(getSampleAssemblyFactory(sampleAssemblyHandler), supervisorProbe)

    doNothing().when(sampleAssemblyHandler).onShutdown()

    running.componentRef ! Lifecycle(ToComponentLifecycleMessage.Shutdown)
    supervisorProbe.expectMsg(ShutdownComplete)
    verify(sampleAssemblyHandler).onShutdown()
  }

  test("A running Assembly component should accept Restart lifecycle message") {
    val sampleAssemblyHandler = mock[AssemblyHandlers[AssemblyDomainMsg]]
    when(sampleAssemblyHandler.initialize()).thenReturn(Future.unit)

    val supervisorProbe = TestProbe[FromComponentLifecycleMessage]
    val running         = run(getSampleAssemblyFactory(sampleAssemblyHandler), supervisorProbe)

    running.componentRef ! Lifecycle(ToComponentLifecycleMessage.Restart)
    Thread.sleep(1000)

    verify(sampleAssemblyHandler).onRestart()
  }

  test("A running Assembly component should accept RunOffline lifecycle message") {
    val sampleAssemblyHandler = mock[AssemblyHandlers[AssemblyDomainMsg]]
    when(sampleAssemblyHandler.initialize()).thenReturn(Future.unit)
    when(sampleAssemblyHandler.isOnline).thenReturn(true)

    val supervisorProbe = TestProbe[FromComponentLifecycleMessage]
    val running         = run(getSampleAssemblyFactory(sampleAssemblyHandler), supervisorProbe)

    running.componentRef ! Lifecycle(ToComponentLifecycleMessage.GoOffline)
    Thread.sleep(1000)

    verify(sampleAssemblyHandler).onGoOffline()
  }

  test("A running Assembly component should not accept RunOffline lifecycle message when it is already offline") {
    val sampleAssemblyHandler = mock[AssemblyHandlers[AssemblyDomainMsg]]
    when(sampleAssemblyHandler.isOnline).thenReturn(false)
    when(sampleAssemblyHandler.initialize()).thenReturn(Future.unit)

    val supervisorProbe = TestProbe[FromComponentLifecycleMessage]
    val running         = run(getSampleAssemblyFactory(sampleAssemblyHandler), supervisorProbe)

    running.componentRef ! Lifecycle(ToComponentLifecycleMessage.GoOffline)
    Thread.sleep(1000)

    verify(sampleAssemblyHandler, never).onGoOffline()
  }

  test("A running Assembly component should accept RunOnline lifecycle message when it is Offline") {
    val sampleAssemblyHandler = mock[AssemblyHandlers[AssemblyDomainMsg]]
    when(sampleAssemblyHandler.isOnline).thenReturn(false)
    when(sampleAssemblyHandler.initialize()).thenReturn(Future.unit)

    val supervisorProbe = TestProbe[FromComponentLifecycleMessage]
    val running         = run(getSampleAssemblyFactory(sampleAssemblyHandler), supervisorProbe)

    running.componentRef ! Lifecycle(ToComponentLifecycleMessage.GoOnline)
    Thread.sleep(1000)

    verify(sampleAssemblyHandler).onGoOnline()
  }

  test("A running Assembly component should not accept RunOnline lifecycle message when it is already Online") {
    val sampleAssemblyHandler = mock[AssemblyHandlers[AssemblyDomainMsg]]
    when(sampleAssemblyHandler.isOnline).thenReturn(true)
    when(sampleAssemblyHandler.initialize()).thenReturn(Future.unit)

    val supervisorProbe = TestProbe[FromComponentLifecycleMessage]
    val running         = run(getSampleAssemblyFactory(sampleAssemblyHandler), supervisorProbe)

    running.componentRef ! Lifecycle(ToComponentLifecycleMessage.GoOnline)
    Thread.sleep(1000)

    verify(sampleAssemblyHandler, never).onGoOnline()
  }
}
