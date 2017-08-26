package csw.common.framework.scaladsl.component

import akka.typed.testkit.StubbedActorContext
import akka.typed.testkit.scaladsl.TestProbe
import csw.common.framework.FrameworkComponentTestSuite
import csw.common.framework.internal.{ComponentBehavior, ComponentMode}
import csw.common.framework.models.IdleMessage.Initialize
import csw.common.framework.models.InitialMessage.Run
import csw.common.framework.models.PreparingToShutdownMessage.ShutdownComplete
import csw.common.framework.models.RunningMessage.Lifecycle
import csw.common.framework.models.SupervisorIdleComponentMessage.{Initialized, Running}
import csw.common.framework.models.{ComponentMessage, FromComponentLifecycleMessage, ToComponentLifecycleMessage}
import csw.common.framework.scaladsl.ComponentHandlers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future

//DEOPSCSW-177-Hooks for lifecycle management
//DEOPSCSW-179-Unique Action for a component
class ComponentLifecycleTest extends FrameworkComponentTestSuite with MockitoSugar {

  class TestData(supervisorProbe: TestProbe[FromComponentLifecycleMessage]) {
    private val ctx = new StubbedActorContext[ComponentMessage]("test-component", 100, system)

    val sampleHcdHandler: ComponentHandlers[ComponentDomainMessage] = mock[ComponentHandlers[ComponentDomainMessage]]
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)
    val componentBehavior = new ComponentBehavior[ComponentDomainMessage](ctx, supervisorProbe.ref, sampleHcdHandler)

    val runningComponent: ComponentBehavior[ComponentDomainMessage] = {
      componentBehavior.onMessage(Initialize)
      supervisorProbe.expectMsgType[Initialized]
      Thread.sleep(100)
      componentBehavior.onMessage(Run)
      supervisorProbe.expectMsgType[Running]
      componentBehavior
    }
  }

  test("A running Hcd component should accept Shutdown lifecycle message") {
    val supervisorProbe = TestProbe[FromComponentLifecycleMessage]
    val testData        = new TestData(supervisorProbe)
    import testData._

    doNothing().when(sampleHcdHandler).onShutdown()

    val previousComponentMode = runningComponent.mode

    runningComponent.onMessage(Lifecycle(ToComponentLifecycleMessage.Shutdown))

    supervisorProbe.expectMsg(ShutdownComplete)
    verify(sampleHcdHandler).onShutdown()
    previousComponentMode shouldBe runningComponent.mode
  }

  test("A running Hcd component should accept Restart lifecycle message") {
    val supervisorProbe = TestProbe[FromComponentLifecycleMessage]
    val testData        = new TestData(supervisorProbe)
    import testData._

    val previousComponentMode = runningComponent.mode

    runningComponent.onMessage(Lifecycle(ToComponentLifecycleMessage.Restart))

    verify(sampleHcdHandler).onRestart()
    previousComponentMode should not be runningComponent.mode
    runningComponent.mode shouldBe ComponentMode.Idle
  }

  test("A running Hcd component should accept RunOffline lifecycle message") {
    val supervisorProbe = TestProbe[FromComponentLifecycleMessage]
    val testData        = new TestData(supervisorProbe)
    import testData._
    when(sampleHcdHandler.isOnline).thenReturn(true)

    val previousComponentMode = runningComponent.mode
    runningComponent.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOffline))
    verify(sampleHcdHandler).onGoOffline()
    verify(sampleHcdHandler).isOnline
    previousComponentMode shouldBe runningComponent.mode
  }

  test("A running Hcd component should not accept RunOffline lifecycle message when it is already offline") {
    val supervisorProbe = TestProbe[FromComponentLifecycleMessage]
    val testData        = new TestData(supervisorProbe)
    import testData._
    when(sampleHcdHandler.isOnline).thenReturn(false)

    val previousComponentMode = runningComponent.mode
    runningComponent.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOffline))
    verify(sampleHcdHandler, never).onGoOffline()
    previousComponentMode shouldBe runningComponent.mode
  }

  test("A running Hcd component should accept RunOnline lifecycle message when it is Offline") {
    val supervisorProbe = TestProbe[FromComponentLifecycleMessage]
    val testData        = new TestData(supervisorProbe)
    import testData._
    when(sampleHcdHandler.isOnline).thenReturn(false)

    val previousComponentMode = runningComponent.mode
    runningComponent.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOnline))
    verify(sampleHcdHandler).onGoOnline()
    previousComponentMode shouldBe runningComponent.mode
  }

  test("A running Hcd component should not accept RunOnline lifecycle message when it is already Online") {
    val supervisorProbe = TestProbe[FromComponentLifecycleMessage]
    val testData        = new TestData(supervisorProbe)
    import testData._

    when(sampleHcdHandler.isOnline).thenReturn(true)

    runningComponent.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOnline))
    verify(sampleHcdHandler, never).onGoOnline()
  }
}
