package csw.trombone.assembly.commands

import akka.actor
import akka.actor.Scheduler
import akka.stream.ActorMaterializer
import akka.typed.ActorRef
import akka.typed.scaladsl.adapter.TypedActorSystemOps
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.util.Timeout
import csw.messages.CommandMessage.Submit
import csw.messages.ccs.CommandIssue.{RequiredHCDUnavailableIssue, WrongInternalStateIssue}
import csw.messages.ccs.commands.CommandResponse.{Completed, NoLongerValid}
import csw.messages.ccs.commands.matchers.DemandMatcher
import csw.messages.ccs.commands.{CommandResponse, ComponentRef, Setup}
import csw.messages.models.PubSub
import csw.messages.params.models.Units.encoder
import csw.messages.params.models.{Id, ObsId}
import csw.trombone.assembly._
import csw.trombone.assembly.actors.TromboneState.TromboneState
import csw.trombone.hcd.TromboneHcdState

import scala.concurrent.Future

class MoveCommand(
    ctx: ActorContext[AssemblyCommandHandlerMsgs],
    ac: AssemblyContext,
    s: Setup,
    tromboneHCD: Option[ComponentRef],
    startState: TromboneState,
    stateActor: ActorRef[PubSub[AssemblyState]]
) extends AssemblyCommand(ctx, startState, stateActor) {

  import csw.trombone.assembly.actors.TromboneState._
  import ctx.executionContext

  implicit val actorSystem: actor.ActorSystem = ctx.system.toUntyped
  implicit val timeout: Timeout               = AssemblyMatchers.idleMatcher.timeout
  implicit val scheduler: Scheduler           = ctx.system.scheduler
  implicit val mat: ActorMaterializer         = ActorMaterializer()

  val stagePosition               = s(ac.stagePositionKey)
  val encoderPosition: Int        = Algorithms.stagePositionToEncoder(ac.controlConfig, stagePosition.head)
  val stateMatcher: DemandMatcher = AssemblyMatchers.posMatcher(encoderPosition)

  val scOut: Setup =
    Setup("originationPrefix", TromboneHcdState.axisMoveCK, s.maybeObsId)
      .add(TromboneHcdState.positionKey -> encoderPosition withUnits encoder)

  def startCommand(): Future[CommandResponse] = {
    if (tromboneHCD.isEmpty)
      Future(NoLongerValid(s.runId, RequiredHCDUnavailableIssue(s"${ac.hcdComponentId} is not available")))
    else if (!(
               startState.cmdChoice == cmdUninitialized ||
               startState.moveChoice != moveIndexed && startState.moveChoice != moveMoving
             )) {
      Future(
        NoLongerValid(s.runId,
                      WrongInternalStateIssue(
                        s"Assembly state of ${startState.cmdChoice}/${startState.moveChoice} does not allow move"
                      ))
      )
    } else {
      publishState(TromboneState(cmdItem(cmdBusy), moveItem(moveMoving), startState.sodiumLayer, startState.nss))

      tromboneHCD.get.onewayAndMatch(scOut, stateMatcher).map {
        case response: Completed ⇒
          publishState(TromboneState(cmdItem(cmdReady), moveItem(moveIndexed), sodiumItem(false), nssItem(false)))
          response
        case otherResponse ⇒ otherResponse
      }
    }
  }

  def stopCommand(): Unit = {
    tromboneHCD.foreach(
      _.component ! Submit(TromboneHcdState.cancelSC(Id(), s.maybeObsId.getOrElse(ObsId.empty)), ctx.spawnAnonymous(Actor.ignore))
    )
  }
}
