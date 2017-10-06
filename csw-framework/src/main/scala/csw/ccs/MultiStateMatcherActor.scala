package csw.ccs

import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import akka.util.Timeout
import csw.ccs.MultiStateMatcherMsgs._
import csw.messages.PubSub.{Subscribe, Unsubscribe}
import csw.messages.params.states.CurrentState
import csw.messages.{CommandExecutionResponse, Completed, Error, PubSub}

object MultiStateMatcherActor {

  def make(currentStateReceiver: ActorRef[PubSub[CurrentState]], timeout: Timeout): Behavior[WaitingMsg] =
    Actor.mutable[MultiStateMatcherMsgs](ctx ⇒ new MultiStateMatcherActor(ctx, currentStateReceiver, timeout)).narrow

  sealed trait Mode
  object Mode {
    case object Waiting   extends Mode
    case object Executing extends Mode
  }

}

class MultiStateMatcherActor(
    ctx: ActorContext[MultiStateMatcherMsgs],
    currentStateReceiver: ActorRef[PubSub[CurrentState]],
    timeout: Timeout
) extends MutableBehavior[MultiStateMatcherMsgs] {

  import MultiStateMatcherActor._

  val currentStateAdapter: ActorRef[CurrentState] = ctx.spawnAdapter(StateUpdate)

  var replyTo: ActorRef[CommandExecutionResponse] = _
  var context: Mode                               = Mode.Waiting
  var timer: Cancellable                          = _
  var matchers: List[StateMatcher]                = _

  currentStateReceiver ! Subscribe(currentStateAdapter)

  def onMessage(msg: MultiStateMatcherMsgs): Behavior[MultiStateMatcherMsgs] = {
    (context, msg) match {
      case (Mode.Waiting, x: WaitingMsg)     ⇒ onWaiting(x)
      case (Mode.Executing, x: ExecutingMsg) ⇒ onExecuting(x)
      case _                                 ⇒ println(s"current context=$context does not handle message=$msg")
    }
    this
  }

  def onWaiting(msg: WaitingMsg): Unit = msg match {
    case StartMatch(replyToIn, matchersIn) =>
      this.replyTo = replyToIn
      this.matchers = matchersIn
      timer = ctx.schedule(timeout.duration, ctx.self, Stop)
      context = Mode.Executing
  }

  def onExecuting(msg: ExecutingMsg): Unit = msg match {
    case StateUpdate(current) =>
      val matched = matchers.filter(_.prefix == current.prefixStr).filter(_.check(current))
      if (matched.nonEmpty) {
        val newMatchers = matchers.diff(matched)
        if (newMatchers.isEmpty) {
          timer.cancel()
          currentStateReceiver ! Unsubscribe(currentStateAdapter)
          replyTo ! Completed
          ctx.stop(currentStateAdapter)
        } else {
          matchers = newMatchers
        }
      }

    case Stop =>
      replyTo ! Error("Current state matching timed out")
      currentStateReceiver ! Unsubscribe(currentStateAdapter)
      ctx.stop(currentStateAdapter)
  }
}
