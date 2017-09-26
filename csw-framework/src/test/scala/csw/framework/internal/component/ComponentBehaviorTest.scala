package csw.framework.internal.component

import akka.typed.testkit.StubbedActorContext
import akka.typed.testkit.scaladsl.TestProbe
import csw.framework.FrameworkTestSuite
import csw.framework.scaladsl.ComponentHandlers
import csw.param.messages.FromComponentLifecycleMessage.{Initialized, Running}
import csw.param.messages.IdleMessage.Initialize
import csw.param.messages.InitialMessage.Run
import csw.param.messages.{ComponentMessage, FromComponentLifecycleMessage}
import csw.services.logging.scaladsl.{ComponentLogger, Logger}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future

// DEOPSCSW-165-CSW Assembly Creation
// DEOPSCSW-166-CSW HCD Creation
class ComponentBehaviorTest extends FrameworkTestSuite with MockitoSugar {

  trait TypedActorMock[T] { this: ComponentLogger.TypedActor[T] ⇒
    override protected lazy val log: Logger = mock[Logger]
  }

  class TestData(supervisorProbe: TestProbe[FromComponentLifecycleMessage]) {
    val sampleComponentHandler: ComponentHandlers[ComponentDomainMessage] =
      mock[ComponentHandlers[ComponentDomainMessage]]
    when(sampleComponentHandler.initialize()).thenReturn(Future.unit)

    val ctx = new StubbedActorContext[ComponentMessage]("test-component", 100, system)
    val componentBehavior =
      new ComponentBehavior[ComponentDomainMessage](ctx, "test-component", supervisorProbe.ref, sampleComponentHandler)
      with TypedActorMock[ComponentMessage]
    when(sampleComponentHandler.initialize()).thenReturn(Future.unit)
    when(sampleComponentHandler.onRun()).thenReturn(Future.unit)
  }

  test("component should start in idle lifecycle state") {
    val supervisorProbe = TestProbe[FromComponentLifecycleMessage]
    val testData        = new TestData(supervisorProbe)
    import testData._

    componentBehavior.lifecycleState shouldBe ComponentLifecycleState.Idle
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
}
