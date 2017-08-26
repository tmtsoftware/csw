package csw.common.framework.scaladsl.component

import akka.typed.testkit.StubbedActorContext
import akka.typed.testkit.scaladsl.TestProbe
import csw.common.framework.FrameworkComponentTestSuite
import csw.common.framework.internal.{ComponentBehavior, ComponentMode}
import csw.common.framework.models.{ComponentMessage, FromComponentLifecycleMessage}
import csw.common.framework.models.IdleMessage.Initialize
import csw.common.framework.models.InitialMessage.Run
import csw.common.framework.models.SupervisorIdleComponentMessage.{InitializeFailure, Initialized, Running}
import csw.common.framework.scaladsl.ComponentHandlers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future
//DEOPSCSW-165-CSW Assembly Creation
//DEOPSCSW-166-CSW HCD Creation
class ComponentBehaviorTest extends FrameworkComponentTestSuite with MockitoSugar {

  class TestData(supervisorProbe: TestProbe[FromComponentLifecycleMessage]) {
    val sampleComponentHandler: ComponentHandlers[ComponentDomainMessage] =
      mock[ComponentHandlers[ComponentDomainMessage]]

    val ctx = new StubbedActorContext[ComponentMessage]("test-component", 100, system)
    val componentBehavior =
      new ComponentBehavior[ComponentDomainMessage](ctx, supervisorProbe.ref, sampleComponentHandler)
    when(sampleComponentHandler.initialize()).thenReturn(Future.unit)
  }

  test("component should start in idle mode") {
    val supervisorProbe = TestProbe[FromComponentLifecycleMessage]
    val testData        = new TestData(supervisorProbe)
    import testData._

    componentBehavior.mode shouldBe ComponentMode.Idle
  }

  test("component should send itself initialize message and handle initialization") {
    val supervisorProbe = TestProbe[FromComponentLifecycleMessage]
    val testData        = new TestData(supervisorProbe)
    import testData._

    ctx.selfInbox.receiveMsg() shouldBe Initialize

    componentBehavior.onMessage(Initialize)

    Thread.sleep(100)

    supervisorProbe.expectMsgType[Initialized]
    verify(sampleComponentHandler).initialize()
  }

  test("component should accept and handle run message from supervisor") {
    val supervisorProbe = TestProbe[FromComponentLifecycleMessage]
    val testData        = new TestData(supervisorProbe)
    import testData._

    componentBehavior.onMessage(Initialize)
    supervisorProbe.expectMsgType[Initialized]
    componentBehavior.onMessage(Run)

    supervisorProbe.expectMsgType[Running]

    verify(sampleComponentHandler).onRun()
    verify(sampleComponentHandler).isOnline_=(true)
  }

  // DEOPSCSW-179: Unique Action for a component
  test("component should send InitializationFailure message if it fails in initialization") {
    val supervisorProbe = TestProbe[FromComponentLifecycleMessage]
    val testData        = new TestData(supervisorProbe)
    import testData._

    val exceptionReason = "test Exception"
    when(sampleComponentHandler.initialize()).thenThrow(new RuntimeException(exceptionReason))
    ctx.selfInbox.receiveMsg() shouldBe Initialize

    componentBehavior.onMessage(Initialize)
    val initializationFailure = supervisorProbe.expectMsgType[InitializeFailure]
    initializationFailure.reason shouldBe exceptionReason
  }
}
