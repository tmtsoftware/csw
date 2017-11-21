package csw.trombone.assembly.commands

import akka.typed.ActorRef
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.ccs.internal.matchers.MatcherResponse.{MatchCompleted, MatchFailed}
import csw.ccs.internal.matchers.PublishedStateMatcher
import csw.messages.CommandMessage.Submit
import csw.messages._
import csw.messages.ccs.CommandIssue.{RequiredHCDUnavailableIssue, WrongInternalStateIssue}
import csw.messages.ccs.commands.CommandResponse.{Completed, Error, NoLongerValid}
import csw.messages.ccs.commands.{CommandResponse, Setup}
import csw.messages.params.models.RunId
import csw.messages.params.models.Units.encoder
import csw.trombone.assembly._
import csw.trombone.assembly.actors.TromboneState.TromboneState
import csw.trombone.hcd.TromboneHcdState

import scala.concurrent.Future

class PositionCommand(
    ctx: ActorContext[AssemblyCommandHandlerMsgs],
    ac: AssemblyContext,
    s: Setup,
    tromboneHCD: Option[ActorRef[SupervisorExternalMessage]],
    startState: TromboneState,
    stateActor: ActorRef[PubSub[AssemblyState]]
) extends AssemblyCommand(ctx, startState, stateActor) {

  import csw.trombone.assembly.actors.TromboneState._
  import ctx.executionContext

  def startCommand(): Future[CommandResponse] = {
    if (tromboneHCD.isEmpty)
      Future(NoLongerValid(s.runId, RequiredHCDUnavailableIssue(s"${ac.hcdComponentId} is not available")))
    if (startState.cmdChoice == cmdUninitialized || startState.moveChoice != moveIndexed && startState.moveChoice != moveMoving) {
      Future(
        NoLongerValid(s.runId,
                      WrongInternalStateIssue(
                        s"Assembly state of ${startState.cmdChoice}/${startState.moveChoice} does not allow datum"
                      ))
      )
    } else {
      val rangeDistance   = s(ac.naRangeDistanceKey)
      val stagePosition   = Algorithms.rangeDistanceToStagePosition(rangeDistance.head)
      val encoderPosition = Algorithms.stagePositionToEncoder(ac.controlConfig, stagePosition)

      println(
        s"Using rangeDistance: ${rangeDistance.head} to get stagePosition: $stagePosition to encoder: $encoderPosition"
      )

      val stateMatcher = AssemblyMatchers.posMatcher(encoderPosition)
      val scOut = Setup(s.obsId, TromboneHcdState.axisMoveCK)
        .add(TromboneHcdState.positionKey -> encoderPosition withUnits encoder)
      publishState(TromboneState(cmdItem(cmdBusy), moveItem(moveIndexing), startState.sodiumLayer, startState.nss))

      tromboneHCD.foreach(_ ! Submit(scOut, ctx.spawnAnonymous(Actor.ignore)))

      new PublishedStateMatcher(ctx).executeMatch(tromboneHCD.get, stateMatcher) {
        case MatchCompleted =>
          publishState(TromboneState(cmdItem(cmdReady), moveItem(moveIndexed), sodiumItem(false), nssItem(false)))
          Completed(s.runId)
        case MatchFailed(ex) =>
          println(s"Data command match failed with error: ${ex.getMessage}")
          Error(s.runId, ex.getMessage)
        case _ ⇒ Error(s.runId, "")
      }
    }
  }

  def stopCommand(): Unit = {
    tromboneHCD.foreach(_ ! Submit(TromboneHcdState.cancelSC(RunId(), s.obsId), ctx.spawnAnonymous(Actor.ignore)))
  }

}
