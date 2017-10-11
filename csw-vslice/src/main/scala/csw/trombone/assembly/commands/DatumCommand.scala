package csw.trombone.assembly.commands

import akka.typed.ActorRef
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.ccs.DemandMatcher
import csw.messages.CommandMessage.Submit
import csw.messages.PubSub.Publish
import csw.messages._
import csw.messages.ccs.ValidationIssue.WrongInternalStateIssue
import csw.messages.ccs.commands.Setup
import csw.trombone.assembly.actors.TromboneState.TromboneState
import csw.trombone.assembly.{Matchers, TromboneCommandHandlerMsgs}
import csw.trombone.hcd.TromboneHcdState

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class DatumCommand(
    ctx: ActorContext[TromboneCommandHandlerMsgs],
    s: Setup,
    tromboneHCD: ActorRef[SupervisorExternalMessage],
    startState: TromboneState,
    stateActor: ActorRef[PubSub[TromboneState]]
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
      sendState(
        TromboneState(cmdItem(cmdBusy), moveItem(moveIndexing), startState.sodiumLayer, startState.nss)
      )
      tromboneHCD ! Submit(Setup(s.info, TromboneHcdState.axisDatumCK), ctx.spawnAnonymous(Actor.ignore))
      Matchers.matchState(ctx, Matchers.idleMatcher, tromboneHCD, 5.seconds).map {
        case Completed =>
          sendState(
            TromboneState(
              cmdItem(cmdReady),
              moveItem(moveIndexed),
              sodiumItem(false),
              nssItem(false)
            )
          )
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

  private def sendState(setState: TromboneState): Unit = {
    stateActor ! Publish(setState)
  }

  override def isAssemblyStateValid: Boolean = ???

  override def sendInvalidCommandResponse: Future[NoLongerValid] = ???

  override def publishInitialState(): Unit = ???

  override def matchState(stateMatcher: DemandMatcher) = ???

  override def sendState(setState: AssemblyState): Unit = ???
}
