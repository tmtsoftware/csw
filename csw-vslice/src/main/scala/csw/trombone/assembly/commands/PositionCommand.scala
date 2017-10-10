package csw.trombone.assembly.commands

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.AskPattern._
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.util.Timeout
import csw.messages.CommandMessage.Submit
import csw.messages._
import csw.messages.ccs.ValidationIssue.WrongInternalStateIssue
import csw.messages.ccs.commands.Setup
import csw.messages.params.models.Units.encoder
import csw.trombone.assembly._
import csw.trombone.assembly.actors.TromboneStateActor.{TromboneState, TromboneStateMsg}
import csw.trombone.hcd.TromboneHcdState

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class PositionCommand(
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
    if (cmd(startState) == cmdUninitialized || (move(startState) != moveIndexed && move(startState) != moveMoving)) {
      Future(
        NoLongerValid(
          WrongInternalStateIssue(s"Assembly state of ${cmd(startState)}/${move(startState)} does not allow datum")
        )
      )
    } else {
      val rangeDistance   = s(ac.naRangeDistanceKey)
      val stagePosition   = Algorithms.rangeDistanceToStagePosition(rangeDistance.head)
      val encoderPosition = Algorithms.stagePositionToEncoder(ac.controlConfig, stagePosition)

      println(
        s"Using rangeDistance: ${rangeDistance.head} to get stagePosition: $stagePosition to encoder: $encoderPosition"
      )

      val stateMatcher = Matchers.posMatcher(encoderPosition)
      val scOut = Setup(s.info, TromboneHcdState.axisMoveCK)
        .add(TromboneHcdState.positionKey -> encoderPosition withUnits encoder)
      sendState(
        SetState(cmdItem(cmdBusy),
                 moveItem(moveIndexing),
                 startState.sodiumLayer,
                 startState.nss,
                 ctx.spawnAnonymous(Actor.ignore))
      )

      tromboneHCD ! Submit(scOut, ctx.spawnAnonymous(Actor.ignore))
      Matchers.matchState(ctx, stateMatcher, tromboneHCD, 5.seconds).map {
        case Completed =>
          sendState(SetState(cmdReady, moveIndexed, sodiumLayer = false, nss = false, ctx.spawnAnonymous(Actor.ignore)))
          Completed
        case Error(message) =>
          println(s"Data command match failed with error: $message")
          Error(message)
        case _ ⇒ Error("")
      }
    }
  }

  def stopCurrentCommand(): Unit = {
    tromboneHCD ! Submit(TromboneHcdState.cancelSC(s.info), ctx.spawnAnonymous(Actor.ignore))
  }

  private def sendState(setState: SetState): Unit = {
    implicit val timeout: Timeout     = Timeout(5.seconds)
    implicit val scheduler: Scheduler = ctx.system.scheduler

    Await.ready(stateActor ? { x: ActorRef[StateWasSet] ⇒
      setState
    }, timeout.duration)
  }

}
