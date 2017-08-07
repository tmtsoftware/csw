package csw.common.framework.internal.supervisor

import akka.typed.ActorSystem
import akka.typed.scaladsl.Actor
import akka.typed.testkit.StubbedActorContext
import csw.common.components.hcd.HcdDomainMsg
import csw.common.framework.models.InitialMsg.Run
import csw.common.framework.models.{InitialMsg, SupervisorMsg}
import csw.common.framework.models.SupervisorIdleMsg.Initialized
import csw.common.framework.scaladsl.{ComponentHandlers, FrameworkComponentTestSuite}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuiteLike, Matchers}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationDouble

class SupervisorTest extends FrameworkComponentTestSuite with FunSuiteLike with Matchers with MockitoSugar {

  ignore("Supervisor") {

    val sampleHcdHandler = mock[ComponentHandlers[HcdDomainMsg]]

    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)

    val supervisor =
      Await.result(
        system.systemActorOf(Supervisor.behavior(hcdInfo, getSampleHcdFactory(sampleHcdHandler)), "sampleSupervisor"),
        5.seconds
      )

    Thread.sleep(2000)

  }

  test("effectful test for supervisor") {
    val sampleHcdHandler = mock[ComponentHandlers[HcdDomainMsg]]
    val system           = ActorSystem("test-system", Actor.empty)
    val ctx              = new StubbedActorContext[SupervisorMsg]("test-supervisor", 100, system)

    val supervisor = new Supervisor(ctx, hcdInfo, getSampleHcdFactory(sampleHcdHandler))

    val supervisorRef = Await.result(system.systemActorOf(supervisor, "sampleSupervisor"), 5.seconds)

    ctx.children.toList.length shouldBe 3

    val childComponentInbox = ctx.childInbox("component")
    supervisorRef ! Initialized(childComponentInbox.ref.upcast)

    Thread.sleep(2000)
    childComponentInbox.hasMessages shouldBe true

    childComponentInbox.receiveMsg().asInstanceOf[InitialMsg] shouldBe Run
  }

}
