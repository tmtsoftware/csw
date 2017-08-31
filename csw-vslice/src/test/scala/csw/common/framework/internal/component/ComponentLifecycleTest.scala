package csw.common.framework.internal.component

import akka.typed.testkit.StubbedActorContext
import akka.typed.testkit.scaladsl.TestProbe
import csw.common.framework.FrameworkTestSuite
import csw.common.framework.exceptions.TriggerRestartException
import csw.common.framework.models.FromComponentLifecycleMessage.{Initialized, Running}
import csw.common.framework.models.IdleMessage.Initialize
import csw.common.framework.models.InitialMessage.Run
import csw.common.framework.models.RunningMessage.Lifecycle
import csw.common.framework.models.{ComponentMessage, FromComponentLifecycleMessage, ToComponentLifecycleMessage}
import csw.common.framework.scaladsl.ComponentHandlers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future

//DEOPSCSW-177-Hooks for lifecycle management
//DEOPSCSW-179-Unique Action for a component
class ComponentLifecycleTest extends FrameworkTestSuite with MockitoSugar {

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

  test("A running Hcd component should handle RunOffline lifecycle message") {
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

  test("A running Hcd component should handle RunOnline lifecycle message when it is Offline") {
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

  test("A running Hcd component should handle Restart lifecycle message when it is Running") {
    val supervisorProbe = TestProbe[FromComponentLifecycleMessage]
    val testData        = new TestData(supervisorProbe)
    import testData._

    intercept[TriggerRestartException] {
      runningComponent.onMessage(Lifecycle(ToComponentLifecycleMessage.Restart))
    }
  }
}
