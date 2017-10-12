package csw.trombone.assembly.commands

import akka.typed.ActorRef
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.messages.CommandMessage.Submit
import csw.messages._
import csw.messages.ccs.ValidationIssue.WrongInternalStateIssue
import csw.messages.ccs.commands.Setup
import csw.trombone.assembly.actors.TromboneState.TromboneState
import csw.trombone.assembly.{AssemblyCommandHandlerMsgs, Matchers}
import csw.trombone.hcd.TromboneHcdState

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class DatumCommand(
    ctx: ActorContext[AssemblyCommandHandlerMsgs],
    s: Setup,
    tromboneHCD: ActorRef[SupervisorExternalMessage],
    startState: TromboneState,
    stateActor: ActorRef[PubSub[AssemblyState]]
) extends AssemblyCommand {

  import csw.trombone.assembly.actors.TromboneState._
  import ctx.executionContext

  def startCommand(): Future[CommandExecutionResponse] = {
    if (startState.cmd.head == cmdUninitialized) {
      Future(
        NoLongerValid(
          WrongInternalStateIssue(
            s"Assembly state of ${startState.cmdChoice}/${startState.moveChoice} does not allow datum"
          )
        )
      )
    } else {
      publishState(
        TromboneState(cmdItem(cmdBusy), moveItem(moveIndexing), startState.sodiumLayer, startState.nss),
        stateActor
      )
      tromboneHCD ! Submit(Setup(s.info, TromboneHcdState.axisDatumCK), ctx.spawnAnonymous(Actor.ignore))
      matchCompletion(ctx, Matchers.idleMatcher, tromboneHCD, 5.seconds) {
        case Completed =>
          publishState(
            TromboneState(cmdItem(cmdReady), moveItem(moveIndexed), sodiumItem(false), nssItem(false)),
            stateActor
          )
          Completed
        case Error(message) =>
          println(s"Data command match failed with error: $message")
          Error(message)
        case _ ⇒ Error("")
      }
    }
  }

  def stopCommand(): Unit = {
    tromboneHCD ! Submit(TromboneHcdState.cancelSC(s.info), ctx.spawnAnonymous(Actor.ignore))
  }

}
