package csw.trombone.assembly.commands

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.messages.PubSub.Publish
import csw.messages._
import csw.messages.ccs.ValidationIssue.WrongInternalStateIssue
import csw.messages.ccs.commands.Setup
import csw.trombone.assembly.actors.TromboneStateActor.{TromboneState, _}
import csw.trombone.assembly.{AssemblyContext, TromboneCommandHandlerMsgs}

import scala.concurrent.Future

class FollowCommand(
    ctx: ActorContext[TromboneCommandHandlerMsgs],
    ac: AssemblyContext,
    s: Setup,
    tromboneHCD: ActorRef[SupervisorExternalMessage],
    startState: TromboneState,
    stateActor: ActorRef[PubSub[TromboneState]]
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
        TromboneState(cmdItem(cmdContinuous),
                      moveItem(moveMoving),
                      sodiumItem(sodiumLayer(startState)),
                      nssItem(s(ac.nssInUseKey).head))
      )
      Future(Completed)
    }
  }

  override def stopCurrentCommand(): Unit = ???

  private def sendState(setState: TromboneState): Unit = {
    stateActor ! Publish(setState)
  }

}
