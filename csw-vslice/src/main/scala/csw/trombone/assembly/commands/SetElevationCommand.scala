package csw.trombone.assembly.commands

import akka.actor.{ActorSystem, Scheduler}
import akka.stream.ActorMaterializer
import akka.typed.ActorRef
import akka.typed.scaladsl.adapter.TypedActorSystemOps
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.util.Timeout
import csw.messages.CommandMessage.Submit
import csw.messages.ccs.CommandIssue.WrongInternalStateIssue
import csw.messages.ccs.commands.CommandResponse.{Accepted, Completed, Error, NoLongerValid}
import csw.messages.ccs.commands.{CommandResponse, Setup, WrappedComponent}
import csw.messages.models.PubSub
import csw.messages.params.models.Units.encoder
import csw.messages.params.models.{ObsId, RunId}
import csw.services.ccs.internal.matchers.Matcher
import csw.services.ccs.internal.matchers.MatcherResponses.{MatchCompleted, MatchFailed}
import csw.trombone.assembly._
import csw.trombone.assembly.actors.TromboneState.TromboneState
import csw.trombone.hcd.TromboneHcdState

import scala.concurrent.Future

class SetElevationCommand(
    ctx: ActorContext[AssemblyCommandHandlerMsgs],
    ac: AssemblyContext,
    s: Setup,
    tromboneHCD: Option[WrappedComponent],
    startState: TromboneState,
    stateActor: ActorRef[PubSub[AssemblyState]]
) extends AssemblyCommand(ctx, startState, stateActor) {

  import TromboneHcdState._
  import csw.trombone.assembly.actors.TromboneState._
  import ctx.executionContext
  implicit val scheduler: Scheduler     = ctx.system.scheduler
  implicit val actorSystem: ActorSystem = ctx.system.toUntyped
  implicit val mat: ActorMaterializer   = ActorMaterializer()

  def startCommand(): Future[CommandResponse] = {
    if (startState.cmdChoice == cmdUninitialized || startState.moveChoice != moveIndexed && startState.moveChoice != moveMoving) {
      Future(
        NoLongerValid(s.runId,
                      WrongInternalStateIssue(
                        s"Assembly state of ${startState.cmdChoice}/${startState.moveChoice} does not allow datum"
                      ))
      )
    } else {
      val elevationItem   = s(ac.naElevationKey)
      val stagePosition   = Algorithms.rangeDistanceToStagePosition(elevationItem.head)
      val encoderPosition = Algorithms.stagePositionToEncoder(ac.controlConfig, stagePosition)

      println(
        s"Using elevation as rangeDistance: ${elevationItem.head} to get stagePosition: $stagePosition to encoder: $encoderPosition"
      )

      val stateMatcher              = AssemblyMatchers.posMatcher(encoderPosition)
      implicit val timeout: Timeout = stateMatcher.timeout

      val scOut = Setup("originationPrefix", axisMoveCK, Some(ac.obsId)).add(positionKey -> encoderPosition withUnits encoder)

      publishState(TromboneState(cmdItem(cmdBusy), moveItem(moveIndexing), startState.sodiumLayer, startState.nss))

      tromboneHCD.get.submit(scOut).flatMap {
        case Accepted(_) ⇒
          new Matcher(tromboneHCD.get.ref, stateMatcher).start.map {
            case MatchCompleted =>
              publishState(TromboneState(cmdItem(cmdReady), moveItem(moveIndexed), sodiumItem(false), nssItem(false)))
              Completed(s.runId)
            case MatchFailed(ex) =>
              println(s"Data command match failed with error: ${ex.getMessage}")
              Error(s.runId, ex.getMessage)
            case _ ⇒ Error(s.runId, "")
          }
        case _ ⇒ Future.successful(Error(s.runId, ""))
      }
    }
  }

  def stopCommand(): Unit = {
    tromboneHCD.foreach(
      _.ref ! Submit(TromboneHcdState.cancelSC(RunId(), s.maybeObsId.getOrElse(ObsId.empty)), ctx.spawnAnonymous(Actor.ignore))
    )
  }
}
