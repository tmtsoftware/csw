package csw.trombone.assembly.commands

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.messages.FromComponentLifecycleMessage.Running
import csw.messages.ccs.ValidationIssue.WrongInternalStateIssue
import csw.messages.ccs.commands.Setup
import csw.messages.{CommandExecutionResponse, Completed, NoLongerValid}
import csw.trombone.assembly.actors.TromboneStateActor.{TromboneState, TromboneStateMsg, _}
import csw.trombone.assembly.{AssemblyContext, TromboneCommandHandlerMsgs}
import akka.typed.scaladsl.AskPattern._
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class FollowCommand(
    ctx: ActorContext[TromboneCommandHandlerMsgs],
    ac: AssemblyContext,
    s: Setup,
    tromboneHCD: Running,
    startState: TromboneState,
    stateActor: Option[ActorRef[TromboneStateMsg]]
) extends TromboneAssemblyCommand {
  import ctx.executionContext
  implicit val scheduler: Scheduler = ctx.system.scheduler
  implicit val timeout: Timeout     = Timeout(5.seconds)
  override def startCommand(): Future[CommandExecutionResponse] = {
    if (cmd(startState) == cmdUninitialized
        || (move(startState) != moveIndexed && move(startState) != moveMoving)
        || !sodiumLayer(startState)) {
      Future(
        NoLongerValid(
          WrongInternalStateIssue(
            s"Assembly state of ${cmd(startState)}/${move(startState)}/${sodiumLayer(startState)} does not allow follow"
          )
        )
      )
    } else {
      (stateActor.get ? { x: ActorRef[StateWasSet] ⇒
        SetState(cmdContinuous,
                 moveMoving,
                 sodiumLayer(startState),
                 s(ac.nssInUseKey).head,
                 ctx.spawnAnonymous(Actor.ignore))
      }).map(_ ⇒ Completed)
    }

  }

  override def stopCurrentCommand(): Unit = ???
}
