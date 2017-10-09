package csw.trombone.assembly.commands

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.AskPattern._
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.util.Timeout
import csw.messages.FromComponentLifecycleMessage.Running
import csw.messages.ccs.ValidationIssue.WrongInternalStateIssue
import csw.messages.ccs.commands.Setup
import csw.messages.{CommandExecutionResponse, Completed, NoLongerValid}
import csw.trombone.assembly.actors.TromboneStateActor.{TromboneState, TromboneStateMsg, _}
import csw.trombone.assembly.{AssemblyContext, TromboneCommandHandlerMsgs}

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Future}

class FollowCommand(
    ctx: ActorContext[TromboneCommandHandlerMsgs],
    ac: AssemblyContext,
    s: Setup,
    tromboneHCD: Running,
    startState: TromboneState,
    stateActor: ActorRef[TromboneStateMsg]
) extends TromboneAssemblyCommand {
  import ctx.executionContext
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
      sendState(
        SetState(cmdContinuous,
                 moveMoving,
                 sodiumLayer(startState),
                 s(ac.nssInUseKey).head,
                 ctx.spawnAnonymous(Actor.ignore))
      )
      Future(Completed)
    }
  }

  override def stopCurrentCommand(): Unit = ???

  private def sendState(setState: SetState): Unit = {
    implicit val timeout: Timeout     = Timeout(5.seconds)
    implicit val scheduler: Scheduler = ctx.system.scheduler
    Await.ready(stateActor ? { x: ActorRef[StateWasSet] ⇒
      setState
    }, timeout.duration)
  }

}
