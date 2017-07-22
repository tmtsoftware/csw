package csw.common.components

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.framework._
import csw.param.Parameters

import scala.concurrent.Future

object TestHcd {
  def behaviour(supervisor: ActorRef[HcdComponentLifecycleMessage]): Behavior[Nothing] =
    Actor.mutable[HcdMsg](ctx ⇒ new TestHcd(ctx, supervisor)).narrow
}

class TestHcd(ctx: ActorContext[HcdMsg], supervisor: ActorRef[HcdComponentLifecycleMessage])
    extends HcdActor[TestHcdMessage](ctx, supervisor) {

  override def initialize(): Future[Unit] = Future.unit

  override def onRun(): Unit = Unit

  override def onShutdown(): Unit = Unit

  override def onShutdownComplete(): Unit = Unit

  override def onLifecycle(x: ToComponentLifecycleMessage): Unit = Unit

  override def onSetup(sc: Parameters.Setup): Unit = Unit

  override def onDomainMsg(msg: TestHcdMessage): Unit = Unit
}
