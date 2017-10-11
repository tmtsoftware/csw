package csw.trombone.assembly.commands

import akka.typed.ActorRef
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.ccs.DemandMatcher
import csw.messages.CommandMessage.Submit
import csw.messages.PubSub.Publish
import csw.messages._
import csw.messages.ccs.ValidationIssue.WrongInternalStateIssue
import csw.messages.ccs.commands.Setup
import csw.messages.params.models.Units.encoder
import csw.trombone.assembly._
import csw.trombone.assembly.actors.TromboneState.TromboneState
import csw.trombone.hcd.TromboneHcdState

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class MoveCommand(
    ctx: ActorContext[TromboneCommandHandlerMsgs],
    ac: AssemblyContext,
    s: Setup,
    tromboneHCD: ActorRef[SupervisorExternalMessage],
    startState: TromboneState,
    stateActor: ActorRef[PubSub[TromboneState]]
) extends AssemblyCommand {

  import csw.trombone.assembly.actors.TromboneState._
  import ctx.executionContext
  val stagePosition   = s(ac.stagePositionKey)
  val encoderPosition = Algorithms.stagePositionToEncoder(ac.controlConfig, stagePosition.head)
  val stateMatcher    = Matchers.posMatcher(encoderPosition)
  val scOut = Setup(s.info, TromboneHcdState.axisMoveCK)
    .add(TromboneHcdState.positionKey -> encoderPosition withUnits encoder)

  def startCommand(): Future[CommandExecutionResponse] = {
    if (!isCommandValid) {
      sendInvalidCommandResponse
    } else {
      publishInitialState()

      tromboneHCD ! Submit(scOut, ctx.spawnAnonymous(Actor.ignore))

      matchState(stateMatcher)
    }
  }

  def stopCurrentCommand(): Unit = {
    tromboneHCD ! Submit(TromboneHcdState.cancelSC(s.info), ctx.spawnAnonymous(Actor.ignore))
  }

  override def matchState(stateMatcher: DemandMatcher): Future[CommandExecutionResponse] = {
    Matchers.matchState(ctx, stateMatcher, tromboneHCD, 5.seconds).map {
      case Completed =>
        sendState(TromboneState(cmdItem(cmdReady), moveItem(moveIndexed), sodiumItem(false), startState.nss))
        Completed
      case Error(message) =>
        println(s"Move command match failed with message: $message")
        Error(message)
      case _ ⇒ Error("")
    }
  }

  override def publishInitialState(): Unit = {
    sendState(TromboneState(cmdItem(cmdBusy), moveItem(moveMoving), startState.sodiumLayer, startState.nss))
  }

  private def isCommandValid: Boolean = {
    startState.cmdChoice == cmdUninitialized || startState.moveChoice != moveIndexed && startState.moveChoice != moveMoving
  }

  override def sendInvalidCommandResponse: Future[NoLongerValid] = {
    Future(
      NoLongerValid(
        WrongInternalStateIssue(
          s"Assembly state of ${startState.cmdChoice}/${startState.moveChoice} does not allow move"
        )
      )
    )
  }

  private def sendState(setState: TromboneState): Unit = {
    stateActor ! Publish(setState)
  }

  override def isAssemblyStateValid: Boolean = ???

  override def sendState(setState: AssemblyState): Unit = ???
}
