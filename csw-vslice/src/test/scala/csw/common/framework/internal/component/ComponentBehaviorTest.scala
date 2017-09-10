package csw.common.framework.internal.component

import akka.typed.testkit.StubbedActorContext
import akka.typed.testkit.scaladsl.TestProbe
import csw.common.framework.FrameworkTestSuite
import csw.common.framework.models.FromComponentLifecycleMessage.Running
import csw.common.framework.models.IdleMessage.Initialize
import csw.common.framework.models.{ComponentMessage, FromComponentLifecycleMessage}
import csw.common.framework.scaladsl.ComponentHandlers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future

// DEOPSCSW-165-CSW Assembly Creation
// DEOPSCSW-166-CSW HCD Creation
class ComponentBehaviorTest extends FrameworkTestSuite with MockitoSugar {

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

    supervisorProbe.expectMsgType[Running]
    verify(sampleComponentHandler).initialize()
    verify(sampleComponentHandler).isOnline_=(true)
  }
}
