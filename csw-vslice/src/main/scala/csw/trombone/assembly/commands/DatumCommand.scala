package csw.trombone.assembly.commands

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.AskPattern._
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.util.Timeout
import csw.messages.CommandMessage.Submit
import csw.messages.FromComponentLifecycleMessage.Running
import csw.messages._
import csw.messages.ccs.ValidationIssue.WrongInternalStateIssue
import csw.messages.ccs.commands.Setup
import csw.messages.params.states.CurrentState
import csw.trombone.assembly.actors.TromboneStateActor.{TromboneState, TromboneStateMsg}
import csw.trombone.assembly.{Matchers, TromboneCommandHandlerMsgs}
import csw.trombone.hcd.TromboneHcdState

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class DatumCommand(
    ctx: ActorContext[TromboneCommandHandlerMsgs],
    s: Setup,
    tromboneHCD: Running,
    startState: TromboneState,
    stateActor: Option[ActorRef[TromboneStateMsg]]
) extends TromboneAssemblyCommand {

  import csw.trombone.assembly.actors.TromboneStateActor._

  private val pubSubRef: ActorRef[PubSub[CurrentState]] = ctx.system.deadLetters
  import ctx.executionContext
  implicit val scheduler: Scheduler = ctx.system.scheduler
  implicit val timeout: Timeout     = Timeout(5.seconds)
  def startCommand(): Future[CommandExecutionResponse] = {
    if (startState.cmd.head == cmdUninitialized) {
      Future(
        NoLongerValid(
          WrongInternalStateIssue(s"Assembly state of ${cmd(startState)}/${move(startState)} does not allow datum")
        )
      )
    } else {
      (stateActor.get ? { x: ActorRef[StateWasSet] ⇒
        SetState(cmdItem(cmdBusy),
                 moveItem(moveIndexing),
                 startState.sodiumLayer,
                 startState.nss,
                 ctx.spawnAnonymous(Actor.ignore))
      }).flatMap { _ =>
        tromboneHCD.componentRef ! Submit(Setup(s.info, TromboneHcdState.axisDatumCK), ctx.spawnAnonymous(Actor.ignore))
        Matchers.matchState(ctx, Matchers.idleMatcher, pubSubRef, 5.seconds).map {
          case Completed =>
            stateActor.foreach(
              _ ! SetState(cmdReady, moveIndexed, sodiumLayer = false, nss = false, ctx.spawnAnonymous(Actor.ignore))
            )
            Completed
          case Error(message) =>
            println(s"Data command match failed with error: $message")
            Error(message)
          case _ ⇒ Error("")
        }
      }
    }
  }

  def stopCurrentCommand(): Unit = {
    tromboneHCD.componentRef ! Submit(TromboneHcdState.cancelSC(s.info), ctx.spawnAnonymous(Actor.ignore))
  }
}
