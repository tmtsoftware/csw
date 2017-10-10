package csw.trombone.assembly.commands

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.AskPattern._
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.util.Timeout
import csw.ccs.DemandMatcher
import csw.messages.CommandMessage.Submit
import csw.messages._
import csw.messages.ccs.ValidationIssue.WrongInternalStateIssue
import csw.messages.ccs.commands.Setup
import csw.messages.params.models.Units.encoder
import csw.trombone.assembly._
import csw.trombone.assembly.actors.TromboneStateActor.{TromboneState, TromboneStateMsg}
import csw.trombone.hcd.TromboneHcdState

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future}

class MoveCommand(
    ctx: ActorContext[TromboneCommandHandlerMsgs],
    ac: AssemblyContext,
    s: Setup,
    tromboneHCD: ActorRef[SupervisorExternalMessage],
    startState: TromboneState,
    stateActor: ActorRef[TromboneStateMsg]
) extends TromboneAssemblyCommand {

  import csw.trombone.assembly.actors.TromboneStateActor._
  import ctx.executionContext

  def startCommand(): Future[CommandExecutionResponse] = {
    if (isCommandValid) {
      sendInvalidCommandResponse
    } else {
      val (stateMatcher: DemandMatcher, scOut: Setup) = publishInitialState

      tromboneHCD ! Submit(scOut, ctx.spawnAnonymous(Actor.ignore))

      matchState(stateMatcher)
    }
  }

  def stopCurrentCommand(): Unit = {
    tromboneHCD ! Submit(TromboneHcdState.cancelSC(s.info), ctx.spawnAnonymous(Actor.ignore))
  }

  private def matchState(stateMatcher: DemandMatcher) = {
    Matchers.matchState(ctx, stateMatcher, tromboneHCD, 5.seconds).map {
      case Completed =>
        sendState(
          SetState(cmdItem(cmdReady),
                   moveItem(moveIndexed),
                   sodiumItem(false),
                   startState.nss,
                   ctx.spawnAnonymous(Actor.ignore))
        )
        Completed
      case Error(message) =>
        println(s"Move command match failed with message: $message")
        Error(message)
      case _ ⇒ Error("")
    }
  }

  private def publishInitialState = {
    val stagePosition   = s(ac.stagePositionKey)
    val encoderPosition = Algorithms.stagePositionToEncoder(ac.controlConfig, stagePosition.head)
    val stateMatcher    = Matchers.posMatcher(encoderPosition)
    val scOut = Setup(s.info, TromboneHcdState.axisMoveCK)
      .add(TromboneHcdState.positionKey -> encoderPosition withUnits encoder)

    sendState(
      SetState(cmdItem(cmdBusy),
               moveItem(moveMoving),
               startState.sodiumLayer,
               startState.nss,
               ctx.spawnAnonymous(Actor.ignore))
    )
    (stateMatcher, scOut)
  }

  private def isCommandValid = {
    cmd(startState) == cmdUninitialized || (move(startState) != moveIndexed && move(startState) != moveMoving)
  }

  private def sendInvalidCommandResponse: Future[NoLongerValid] = {
    Future(
      NoLongerValid(
        WrongInternalStateIssue(s"Assembly state of ${cmd(startState)}/${move(startState)} does not allow move")
      )
    )
  }

  private def sendState(setState: SetState): Unit = {
    implicit val timeout: Timeout     = Timeout(5.seconds)
    implicit val scheduler: Scheduler = ctx.system.scheduler

    Await.ready(stateActor ? { x: ActorRef[StateWasSet] ⇒
      setState
    }, timeout.duration)
  }
}
