package csw.trombone.assembly.commands

import akka.typed.ActorRef
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.messages.CommandMessage.Submit
import csw.messages._
import csw.messages.ccs.ValidationIssue.{RequiredHCDUnavailableIssue, WrongInternalStateIssue}
import csw.messages.ccs.commands.Setup
import csw.messages.params.models.RunId
import csw.trombone.assembly.actors.TromboneState.TromboneState
import csw.trombone.assembly.{AssemblyCommandHandlerMsgs, AssemblyContext, Matchers}
import csw.trombone.hcd.TromboneHcdState

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class DatumCommand(ctx: ActorContext[AssemblyCommandHandlerMsgs],
                   ac: AssemblyContext,
                   s: Setup,
                   tromboneHCD: Option[ActorRef[SupervisorExternalMessage]],
                   startState: TromboneState,
                   stateActor: ActorRef[PubSub[AssemblyState]])
    extends AssemblyCommand(ctx, startState, stateActor) {

  import csw.trombone.assembly.actors.TromboneState._
  import ctx.executionContext

  def startCommand(): Future[CommandExecutionResponse] = {
    if (tromboneHCD.isEmpty)
      Future(NoLongerValid(RequiredHCDUnavailableIssue(s"${ac.hcdComponentId} is not available")))
    if (startState.cmd.head == cmdUninitialized) {
      Future(
        NoLongerValid(
          WrongInternalStateIssue(
            s"Assembly state of ${startState.cmdChoice}/${startState.moveChoice} does not allow datum"
          )
        )
      )
    } else {
      publishState(TromboneState(cmdItem(cmdBusy), moveItem(moveIndexing), startState.sodiumLayer, startState.nss))
      tromboneHCD.foreach(
        _ ! Submit(Setup(RunId(), s.obsId, TromboneHcdState.axisDatumCK), ctx.spawnAnonymous(Actor.ignore))
      )
      matchCompletion(Matchers.idleMatcher, tromboneHCD.get, 5.seconds) {
        case Completed =>
          publishState(TromboneState(cmdItem(cmdReady), moveItem(moveIndexed), sodiumItem(false), nssItem(false)))
          Completed
        case Error(message) =>
          println(s"Data command match failed with error: $message")
          Error(message)
        case _ ⇒ Error("")
      }
    }
  }

  def stopCommand(): Unit = {
    tromboneHCD.foreach(_ ! Submit(TromboneHcdState.cancelSC(RunId(), s.obsId), ctx.spawnAnonymous(Actor.ignore)))
  }

}
