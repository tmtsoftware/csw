package csw.trombone.assembly.commands

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.util.Timeout
import csw.messages.CommandMessage.Submit
import csw.messages.FromComponentLifecycleMessage.Running
import csw.messages._
import csw.messages.ccs.ValidationIssue.WrongInternalStateIssue
import csw.messages.ccs.commands.Setup
import csw.messages.params.models.Units.encoder
import csw.messages.params.states.CurrentState
import csw.trombone.assembly._
import csw.trombone.assembly.actors.TromboneStateActor.{TromboneState, TromboneStateMsg}
import csw.trombone.hcd.TromboneHcdState
import akka.typed.scaladsl.AskPattern._

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future}

class SetElevationCommand(
    ctx: ActorContext[TromboneCommandHandlerMsgs],
    ac: AssemblyContext,
    s: Setup,
    tromboneHCD: Running,
    startState: TromboneState,
    stateActor: ActorRef[TromboneStateMsg]
) extends TromboneAssemblyCommand {

  import TromboneHcdState._
  import csw.trombone.assembly.actors.TromboneStateActor._
  import ctx.executionContext

  private val pubSubRef: ActorRef[PubSub[CurrentState]] = ctx.system.deadLetters

  def startCommand(): Future[CommandExecutionResponse] = {
    if (cmd(startState) == cmdUninitialized || (move(startState) != moveIndexed && move(startState) != moveMoving)) {
      Future(
        NoLongerValid(
          WrongInternalStateIssue(s"Assembly state of ${cmd(startState)}/${move(startState)} does not allow datum")
        )
      )
    } else {
      val elevationItem   = s(ac.naElevationKey)
      val stagePosition   = Algorithms.rangeDistanceToStagePosition(elevationItem.head)
      val encoderPosition = Algorithms.stagePositionToEncoder(ac.controlConfig, stagePosition)

      println(
        s"Using elevation as rangeDistance: ${elevationItem.head} to get stagePosition: $stagePosition to encoder: $encoderPosition"
      )

      val stateMatcher = Matchers.posMatcher(encoderPosition)
      val scOut        = Setup(ac.commandInfo, axisMoveCK).add(positionKey -> encoderPosition withUnits encoder)

      sendState(
        SetState(cmdItem(cmdBusy),
                 moveItem(moveIndexing),
                 startState.sodiumLayer,
                 startState.nss,
                 ctx.spawnAnonymous(Actor.ignore))
      )
      tromboneHCD.componentRef ! Submit(scOut, ctx.spawnAnonymous(Actor.ignore))
      Matchers.matchState(ctx, stateMatcher, pubSubRef, 5.seconds).map {
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
    tromboneHCD.componentRef ! Submit(TromboneHcdState.cancelSC(s.info), ctx.spawnAnonymous(Actor.ignore))
  }

  private def sendState(setState: SetState): Unit = {
    implicit val timeout: Timeout     = Timeout(5.seconds)
    implicit val scheduler: Scheduler = ctx.system.scheduler

    Await.ready(stateActor ? { x: ActorRef[StateWasSet] ⇒
      setState
    }, timeout.duration)
  }
}
