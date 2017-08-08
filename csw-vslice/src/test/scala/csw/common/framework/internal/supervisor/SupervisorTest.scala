package csw.common.framework.internal.supervisor

import akka.typed.ActorSystem
import akka.typed.scaladsl.Actor
import akka.typed.testkit.StubbedActorContext
import csw.common.components.hcd.HcdDomainMsg
import csw.common.framework.models.InitialMsg.Run
import csw.common.framework.models.SupervisorIdleMsg.Initialized
import csw.common.framework.models.{InitialMsg, SupervisorMsg}
import csw.common.framework.scaladsl.{ComponentHandlers, FrameworkComponentTestSuite}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuiteLike, Matchers}

class SupervisorTest extends FrameworkComponentTestSuite with FunSuiteLike with Matchers with MockitoSugar {

  test("effectful test for supervisor") {
    val sampleHcdHandler = mock[ComponentHandlers[HcdDomainMsg]]
    val system           = ActorSystem("test-system", Actor.empty)
    val ctx              = new StubbedActorContext[SupervisorMsg]("test-supervisor", 100, system)

    val supervisor = new Supervisor(ctx, hcdInfo, getSampleHcdFactory(sampleHcdHandler))

    ctx.children.toList.length shouldBe 3

    val childComponentInbox = ctx.childInbox[InitialMsg](supervisor.component.upcast)

    supervisor.onMessage(Initialized(childComponentInbox.ref.upcast))
    childComponentInbox.hasMessages shouldBe true
    childComponentInbox.receiveMsg() shouldBe Run

  }
}
