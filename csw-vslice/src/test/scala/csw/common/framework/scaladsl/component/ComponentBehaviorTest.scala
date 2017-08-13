package csw.common.framework.scaladsl.component

import akka.typed.testkit.StubbedActorContext
import akka.typed.testkit.scaladsl.TestProbe
import csw.common.framework.internal.{ComponentBehavior, ComponentMode}
import csw.common.framework.models.{ComponentMsg, FromComponentLifecycleMessage}
import csw.common.framework.models.IdleMsg.Initialize
import csw.common.framework.models.InitialMsg.Run
import csw.common.framework.models.SupervisorIdleMsg.{InitializeFailure, Initialized, Running}
import csw.common.framework.scaladsl.{ComponentHandlers, FrameworkComponentTestSuite}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future

class ComponentBehaviorTest extends FrameworkComponentTestSuite with MockitoSugar {
  class TestData(
      supervisorProbe: TestProbe[FromComponentLifecycleMessage]
  ) {
    val sampleComponentHandler: ComponentHandlers[ComponentDomainMsg] = mock[ComponentHandlers[ComponentDomainMsg]]
    val ctx                                                           = new StubbedActorContext[ComponentMsg]("test-component", 100, system)

    val componentBehavior = new ComponentBehavior[ComponentDomainMsg](
      ctx,
      supervisorProbe.ref,
      sampleComponentHandler
    )

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
