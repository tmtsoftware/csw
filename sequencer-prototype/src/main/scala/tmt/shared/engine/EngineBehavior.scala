package tmt.shared.engine

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.messages.commands.ControlCommand
import tmt.shared.engine.EngineBehavior._

import scala.collection.immutable.Queue

class EngineBehavior(ctx: ActorContext[EngineAction]) extends MutableBehavior[EngineAction] {

  var queue: Queue[ControlCommand]          = Queue.empty
  var ref: Option[ActorRef[ControlCommand]] = None
  var paused: Boolean                       = false

  override def onMessage(msg: EngineAction): Behavior[EngineAction] = {
    msg match {
      case Push(x) if ref.isEmpty || paused =>
        queue = queue.enqueue(x)
      case Push(x) =>
        ref.foreach(_ ! x)
        ref = None
      case Pull(replyTo) if hasNext =>
        val (elm, q) = queue.dequeue
        replyTo ! elm
        queue = q
      case Pull(replyTo)    => ref = Some(replyTo)
      case HasNext(replyTo) => replyTo ! hasNext
      case Pause            => paused = true
      case Resume =>
        paused = false
        ref.foreach(x => ctx.self ! Pull(x))
        ref = None
      case Reset =>
        queue = Queue.empty
    }
    this
  }

  def hasNext: Boolean = queue.nonEmpty && !paused
}

object EngineBehavior {
  sealed trait EngineAction
  case class Push(command: ControlCommand)           extends EngineAction
  case class Pull(replyTo: ActorRef[ControlCommand]) extends EngineAction
  case class HasNext(replyTo: ActorRef[Boolean])     extends EngineAction
  case object Pause                                  extends EngineAction
  case object Resume                                 extends EngineAction
  case object Reset                                  extends EngineAction

  def behavior: Behavior[EngineAction] = Actor.mutable(ctx => new EngineBehavior(ctx))
}
