package csw.trombone.assembly.commands

import akka.actor
import akka.actor.Scheduler
import akka.stream.ActorMaterializer
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter.TypedActorSystemOps
import akka.util.Timeout
import csw.messages.ccs.CommandIssue.{RequiredHCDUnavailableIssue, WrongInternalStateIssue}
import csw.messages.ccs.commands.CommandResponse.{Completed, NoLongerValid}
import csw.messages.ccs.commands.{CommandResponse, ComponentRef, Setup}
import csw.messages.models.PubSub
import csw.messages.params.models.{Id, ObsId, Prefix}
import csw.trombone.assembly.actors.TromboneState.TromboneState
import csw.trombone.assembly.{AssemblyCommandHandlerMsgs, AssemblyContext, AssemblyMatchers}
import csw.trombone.hcd.TromboneHcdState

import scala.concurrent.Future

class DatumCommand(
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

  def startCommand(): Future[CommandResponse] = {
    if (tromboneHCD.isEmpty)
      Future(NoLongerValid(s.runId, RequiredHCDUnavailableIssue(s"${ac.hcdComponentId} is not available")))
    if (startState.cmd.head == cmdUninitialized) {
      Future(
        NoLongerValid(s.runId,
                      WrongInternalStateIssue(
                        s"Assembly state of ${startState.cmdChoice}/${startState.moveChoice} does not allow datum"
                      ))
      )
    } else {
      publishState(TromboneState(cmdItem(cmdBusy), moveItem(moveIndexing), startState.sodiumLayer, startState.nss))

      val setup = Setup(Prefix("sourcePrefix"), TromboneHcdState.axisDatumCK, s.maybeObsId)

      tromboneHCD.get.onewayAndMatch(setup, AssemblyMatchers.idleMatcher).map {
        case response: Completed ⇒
          publishState(TromboneState(cmdItem(cmdReady), moveItem(moveIndexed), sodiumItem(false), nssItem(false)))
          response
        case otherResponse ⇒ otherResponse
      }
    }
  }

  def stopCommand(): Unit = {
    tromboneHCD.foreach(
      _.submit(TromboneHcdState.cancelSC(Id(), s.maybeObsId.getOrElse(ObsId.empty)))
    )
  }

}
