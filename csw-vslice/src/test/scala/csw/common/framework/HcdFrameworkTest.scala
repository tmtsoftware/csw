package csw.common.framework

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import csw.common.framework.HcdComponentLifecycleMessage.{Initialized, Running}
import csw.common.framework.InitialHcdMsg.Run
import csw.param.Parameters
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

sealed trait TestHcdMessage extends DomainMsg

object TestHcd {
  def behaviour(supervisor: ActorRef[HcdComponentLifecycleMessage]): Behavior[Nothing] =
    Actor.mutable[HcdMsg](ctx ⇒ new TestHcd(ctx, supervisor)).narrow
}

class TestHcd(ctx: ActorContext[HcdMsg], supervisor: ActorRef[HcdComponentLifecycleMessage])
    extends HcdActor[TestHcdMessage](ctx, supervisor) {

  override def initialize(): Future[Unit] = Future.unit

  override def onRun(): Unit = Unit

  override def onShutdown(): Unit = ???

  override def onShutdownComplete(): Unit = ???

  override def onLifecycle(x: ToComponentLifecycleMessage): Unit = ???

  override def onSetup(sc: Parameters.Setup): Unit = ???

  override def onDomainMsg(msg: TestHcdMessage): Unit = ???
}

class HcdFrameworkTest extends FunSuite with Matchers {

  test("hcd") {
    implicit val system   = ActorSystem("testHcd", Actor.empty)
    implicit val settings = TestKitSettings(system)
    implicit val timeout  = Timeout(5.seconds)

    val testProbe: TestProbe[HcdComponentLifecycleMessage] = TestProbe[HcdComponentLifecycleMessage]

    val hcdRef =
      Await.result(system.systemActorOf[Nothing](TestHcd.behaviour(testProbe.ref), "Hcd"), 5.seconds)

    val initialized = testProbe.expectMsgType[Initialized]
    initialized.hcdRef shouldBe hcdRef

    initialized.hcdRef ! Run(testProbe.ref)
    testProbe.expectMsgType[Running]
  }
}
