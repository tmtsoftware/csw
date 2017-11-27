package csw.trombone.assembly.commands

import akka.actor
import akka.actor.Scheduler
import akka.stream.ActorMaterializer
import akka.typed.ActorRef
import akka.typed.scaladsl.adapter.TypedActorSystemOps
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.util.Timeout
import csw.messages.CommandMessage.Submit
import csw.messages._
import csw.messages.ccs.CommandIssue.{RequiredHCDUnavailableIssue, WrongInternalStateIssue}
import csw.messages.ccs.commands.CommandResponse.{Accepted, Completed, Error, NoLongerValid}
import csw.messages.ccs.commands.{CommandResponse, Setup}
import csw.messages.models.PubSub
import csw.messages.params.models.RunId
import csw.services.ccs.common.ActorRefExts.RichActor
import csw.services.ccs.internal.matchers.MatcherResponse.{MatchCompleted, MatchFailed}
import csw.services.ccs.internal.matchers.Matcher
import csw.trombone.assembly.actors.TromboneState.TromboneState
import csw.trombone.assembly.{AssemblyCommandHandlerMsgs, AssemblyContext, AssemblyMatchers}
import csw.trombone.hcd.TromboneHcdState

import scala.concurrent.Future

class DatumCommand(
    ctx: ActorContext[AssemblyCommandHandlerMsgs],
    ac: AssemblyContext,
    s: Setup,
    tromboneHCD: Option[ActorRef[SupervisorExternalMessage]],
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

      tromboneHCD.get
        .ask[CommandResponse](Submit(Setup(s.obsId, TromboneHcdState.axisDatumCK), _))
        .flatMap {
          case _: Accepted ⇒
            new Matcher(tromboneHCD.get, AssemblyMatchers.idleMatcher).response.map {
              case MatchCompleted =>
                publishState(TromboneState(cmdItem(cmdReady), moveItem(moveIndexed), sodiumItem(false), nssItem(false)))
                Completed(s.runId)
              case MatchFailed(ex) =>
                println(s"Data command match failed with error: ${ex.getMessage}")
                Error(s.runId, ex.getMessage)
            }
          case _ => Future.successful(Error(s.runId, ""))
        }

    }
  }

  def stopCommand(): Unit = {
    tromboneHCD.foreach(_ ! Submit(TromboneHcdState.cancelSC(RunId(), s.obsId), ctx.spawnAnonymous(Actor.ignore)))
  }

}
