package csw.trombone.assembly.commands

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.messages.CommandMessage.Submit
import csw.messages.FromComponentLifecycleMessage.Running
import csw.messages.ccs.commands.Setup
import csw.messages.ccs.ValidationIssue.WrongInternalStateIssue
import csw.messages.params.states.CurrentState
import csw.messages._
import csw.trombone.assembly.Matchers
import csw.trombone.assembly.actors.TromboneStateActor.{TromboneState, TromboneStateMsg}
import csw.trombone.hcd.TromboneHcdState
import csw.trombone.messages.CommandMsgs
import csw.trombone.messages.CommandMsgs.{CommandStart, SetStateResponseE, StopCurrentCommand}

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

object DatumCommand {
  def make(
      s: Setup,
      tromboneHCD: Running,
      startState: TromboneState,
      stateActor: Option[ActorRef[TromboneStateMsg]]
  ): Behavior[CommandMsgs] =
    Actor.mutable(ctx ⇒ new DatumCommand(ctx, s, tromboneHCD, startState, stateActor))
}

class DatumCommand(
    ctx: ActorContext[CommandMsgs],
    s: Setup,
    tromboneHCD: Running,
    startState: TromboneState,
    stateActor: Option[ActorRef[TromboneStateMsg]]
) extends MutableBehavior[CommandMsgs] {

  import csw.trombone.assembly.actors.TromboneStateActor._

  private val setStateResponseAdapter: ActorRef[StateWasSet] = ctx.spawnAdapter(SetStateResponseE)
  private val pubSubRef: ActorRef[PubSub[CurrentState]]      = ctx.system.deadLetters

  def startCommand(): CommandExecutionResponse = {
    if (startState.cmd.head == cmdUninitialized) {
      NoLongerValid(
        WrongInternalStateIssue(s"Assembly state of ${cmd(startState)}/${move(startState)} does not allow datum")
      )
    } else {
      stateActor.foreach(
        _ ! SetState(cmdItem(cmdBusy),
                     moveItem(moveIndexing),
                     startState.sodiumLayer,
                     startState.nss,
                     setStateResponseAdapter)
      )
      tromboneHCD.componentRef ! Submit(Setup(s.info, TromboneHcdState.axisDatumCK), ctx.spawnAnonymous(Actor.ignore))
      val response = Await.result(Matchers.matchState(ctx, Matchers.idleMatcher, pubSubRef, 5.seconds), 5.seconds)
      response match {
        case Completed =>
          stateActor.foreach(
            _ ! SetState(cmdReady, moveIndexed, sodiumLayer = false, nss = false, setStateResponseAdapter)
          )
        case Error(message) =>
          println(s"Data command match failed with error: $message")
      }
      response
    }
  }

  override def onMessage(msg: CommandMsgs): Behavior[CommandMsgs] = msg match {
    case CommandStart(replyTo) =>
      if (startState.cmd.head == cmdUninitialized) {
        replyTo ! NoLongerValid(
          WrongInternalStateIssue(s"Assembly state of ${cmd(startState)}/${move(startState)} does not allow datum")
        )
      } else {
        stateActor.foreach(
          _ ! SetState(cmdItem(cmdBusy),
                       moveItem(moveIndexing),
                       startState.sodiumLayer,
                       startState.nss,
                       setStateResponseAdapter)
        )
        tromboneHCD.componentRef ! Submit(Setup(s.info, TromboneHcdState.axisDatumCK), ctx.spawnAnonymous(Actor.ignore))
        Matchers.executeMatch(ctx, Matchers.idleMatcher, pubSubRef, Some(replyTo)) {
          case Completed =>
            stateActor.foreach(
              _ ! SetState(cmdReady, moveIndexed, sodiumLayer = false, nss = false, setStateResponseAdapter)
            )
          case Error(message) =>
            println(s"Data command match failed with error: $message")
        }
      }
      this
    case StopCurrentCommand =>
      tromboneHCD.componentRef ! Submit(TromboneHcdState.cancelSC(s.info), ctx.spawnAnonymous(Actor.ignore))
      this

    case SetStateResponseE(response: StateWasSet) => // ignore confirmation
      this
  }
}
