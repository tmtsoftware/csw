package csw.common.framework.internal.component

import akka.typed.testkit.StubbedActorContext
import akka.typed.testkit.scaladsl.TestProbe
import csw.common.framework.FrameworkTestSuite
import csw.common.framework.models.FromComponentLifecycleMessage.Running
import csw.common.framework.models.IdleMessage.Initialize
import csw.common.framework.models.{ComponentMessage, FromComponentLifecycleMessage}
import csw.common.framework.scaladsl.ComponentHandlers
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
    when(sampleComponentHandler.componentName).thenReturn("test-component")

    val ctx = new StubbedActorContext[ComponentMessage]("test-component", 100, system)
    val componentBehavior =
      new ComponentBehavior[ComponentDomainMessage](ctx, supervisorProbe.ref, sampleComponentHandler)
      with TypedActorMock[ComponentMessage]
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

    supervisorProbe.expectMsgType[Running]
    verify(sampleComponentHandler).initialize()
    verify(sampleComponentHandler).isOnline_=(true)
  }
}
