package csw.trombone.assembly.commands

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.ccs.DemandMatcher
import csw.messages.PubSub.Publish
import csw.messages._
import csw.messages.ccs.ValidationIssue.WrongInternalStateIssue
import csw.messages.ccs.commands.Setup
import csw.trombone.assembly.actors.TromboneState.{TromboneState, _}
import csw.trombone.assembly.{AssemblyContext, TromboneCommandHandlerMsgs}

import scala.concurrent.Future

class FollowCommand(
    ctx: ActorContext[TromboneCommandHandlerMsgs],
    ac: AssemblyContext,
    s: Setup,
    tromboneHCD: ActorRef[SupervisorExternalMessage],
    startState: TromboneState,
    stateActor: ActorRef[PubSub[TromboneState]]
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
      sendState(
        TromboneState(cmdItem(cmdContinuous),
                      moveItem(moveMoving),
                      sodiumItem(startState.sodiumLayerValue),
                      nssItem(s(ac.nssInUseKey).head))
      )
      Future(Completed)
    }
  }

  override def stopCurrentCommand(): Unit = ???

  private def sendState(setState: TromboneState): Unit = {
    stateActor ! Publish(setState)
  }

  override def isAssemblyStateValid: Boolean = ???

  override def sendInvalidCommandResponse: Future[NoLongerValid] = ???

  override def publishInitialState(): Unit = ???

  override def matchState(stateMatcher: DemandMatcher) = ???

  override def sendState(setState: AssemblyState): Unit = ???
}
