package csw.common.framework.scaladsl.hcd

import akka.typed.testkit.StubbedActorContext
import akka.typed.testkit.scaladsl.TestProbe
import csw.common.components.hcd.HcdDomainMsg
import csw.common.framework.internal.ComponentBehavior
import csw.common.framework.models.IdleMsg.Initialize
import csw.common.framework.models.InitialMsg.Run
import csw.common.framework.models.SupervisorIdleMsg.{InitializeFailure, Initialized, Running}
import csw.common.framework.models.{ComponentMsg, FromComponentLifecycleMessage}
import csw.common.framework.scaladsl.{ComponentHandlers, FrameworkComponentTestSuite}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future

class StubbedHcdBehaviorTest extends FrameworkComponentTestSuite with MockitoSugar {

  test("hcd component should send initialize and running message to supervisor") {
    val sampleHcdHandler = mock[ComponentHandlers[HcdDomainMsg]]

    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)

    val supervisorProbe: TestProbe[FromComponentLifecycleMessage] = TestProbe[FromComponentLifecycleMessage]
    val ctx                                                       = new StubbedActorContext[ComponentMsg]("test-component", 100, system)
    val componentBehavior                                         = new ComponentBehavior[HcdDomainMsg](ctx, supervisorProbe.ref, sampleHcdHandler)

    ctx.selfInbox.receiveMsg() shouldBe Initialize

    componentBehavior.onMessage(Initialize)

    Thread.sleep(100)

    supervisorProbe.expectMsgType[Initialized]
    verify(sampleHcdHandler).initialize()

    componentBehavior.onMessage(Run)

    supervisorProbe.expectMsgType[Running]

    verify(sampleHcdHandler).onRun()
    verify(sampleHcdHandler).isOnline_=(true)
  }

  test("A Hcd component should send InitializationFailure message if it fails in initialization") {
    val sampleHcdHandler = mock[ComponentHandlers[HcdDomainMsg]]
    val exceptionReason  = "test Exception"
    when(sampleHcdHandler.initialize()).thenThrow(new RuntimeException(exceptionReason))

    val supervisorProbe: TestProbe[FromComponentLifecycleMessage] = TestProbe[FromComponentLifecycleMessage]
    val ctx                                                       = new StubbedActorContext[ComponentMsg]("test-component", 100, system)
    val componentBehavior                                         = new ComponentBehavior[HcdDomainMsg](ctx, supervisorProbe.ref, sampleHcdHandler)

    ctx.selfInbox.receiveMsg() shouldBe Initialize

    componentBehavior.onMessage(Initialize)
    val initializationFailure = supervisorProbe.expectMsgType[InitializeFailure]
    initializationFailure.reason shouldBe exceptionReason
  }
}
