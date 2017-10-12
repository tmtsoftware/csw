package csw.trombone.assembly.commands

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.messages._
import csw.messages.ccs.ValidationIssue.WrongInternalStateIssue
import csw.messages.ccs.commands.Setup
import csw.trombone.assembly.actors.TromboneState.{TromboneState, _}
import csw.trombone.assembly.{AssemblyCommandHandlerMsgs, AssemblyContext}

import scala.concurrent.Future

class FollowCommand(
    ctx: ActorContext[AssemblyCommandHandlerMsgs],
    ac: AssemblyContext,
    s: Setup,
    tromboneHCD: ActorRef[SupervisorExternalMessage],
    startState: TromboneState,
    stateActor: ActorRef[PubSub[AssemblyState]]
) extends AssemblyCommand {

  import ctx.executionContext

  override def startCommand(): Future[CommandExecutionResponse] = {
    if (startState.cmdChoice == cmdUninitialized
        || startState.moveChoice != moveIndexed && startState.moveChoice != moveMoving
        || !startState.sodiumLayerValue) {
      Future(
        NoLongerValid(
          WrongInternalStateIssue(
            s"Assembly state of ${startState.cmdChoice}/${startState.moveChoice}/${startState.sodiumLayerValue} does not allow follow"
          )
        )
      )
    } else {
      publishState(
        TromboneState(cmdItem(cmdContinuous),
                      moveItem(moveMoving),
                      sodiumItem(startState.sodiumLayerValue),
                      nssItem(s(ac.nssInUseKey).head)),
        stateActor
      )
      Future(Completed)
    }
  }

  override def stopCommand(): Unit = ???

}
