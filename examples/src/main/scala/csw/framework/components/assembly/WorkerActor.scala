package csw.framework.components.assembly

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors.MutableBehavior
import akka.actor.typed.{ActorRef, Behavior}
import csw.services.config.api.models.ConfigData

trait WorkerActorMsg
object WorkerActorMsgs {
  case class InitialState(replyTo: ActorRef[Int])      extends WorkerActorMsg
  case class JInitialState(replyTo: ActorRef[Integer]) extends WorkerActorMsg
  case class GetStatistics(replyTo: ActorRef[Int])     extends WorkerActorMsg
}

object WorkerActor {
  def make(configData: ConfigData): Behavior[WorkerActorMsg] =
    Behaviors.mutable(ctx ⇒ new WorkerActor(ctx, configData: ConfigData))
}

class WorkerActor(ctx: ActorContext[WorkerActorMsg], configData: ConfigData) extends MutableBehavior[WorkerActorMsg] {
  override def onMessage(msg: WorkerActorMsg): Behavior[WorkerActorMsg] = ???
}
