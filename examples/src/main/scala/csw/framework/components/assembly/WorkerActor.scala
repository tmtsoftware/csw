package csw.framework.components.assembly

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.scaladsl.Actor.MutableBehavior
import csw.services.config.api.models.ConfigData

trait WorkerActorMsg
object WorkerActorMsgs {
  case class InitialState(replyTo: ActorRef[Int])      extends WorkerActorMsg
  case class JInitialState(replyTo: ActorRef[Integer]) extends WorkerActorMsg
  case class GetStatistics(replyTo: ActorRef[Int])     extends WorkerActorMsg
}

object WorkerActor {
  def make(configData: ConfigData): Behavior[WorkerActorMsg] =
    Actor.mutable(ctx ⇒ new WorkerActor(ctx, configData: ConfigData))
}

class WorkerActor(ctx: ActorContext[WorkerActorMsg], configData: ConfigData) extends MutableBehavior[WorkerActorMsg] {
  override def onMessage(msg: WorkerActorMsg): Behavior[WorkerActorMsg] = ???
}
