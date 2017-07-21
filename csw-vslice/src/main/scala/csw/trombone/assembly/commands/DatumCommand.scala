package csw.trombone.assembly.commands

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.param.Parameters.Setup
import csw.trombone.assembly.actors.TromboneCommandHandler
import csw.trombone.assembly.actors.TromboneStateActor.{TromboneState, TromboneStateMsg}
import csw.common.ccs.CommandStatus.{Completed, Error, NoLongerValid}
import csw.common.ccs.Validation.WrongInternalStateIssue
import csw.common.framework.CommandMsgs
import csw.common.framework.CommandMsgs.{CommandStart, SetStateResponseE, StopCurrentCommand}
import csw.common.framework.HcdComponentLifecycleMessage.Running
import csw.common.framework.RunningHcdMsg.Submit
import csw.trombone.hcd.TromboneHcdState

object DatumCommand {
  def make(s: Setup,
           tromboneHCD: Running,
           startState: TromboneState,
           stateActor: Option[ActorRef[TromboneStateMsg]]): Behavior[CommandMsgs] =
    Actor.mutable(ctx ⇒ new DatumCommand(ctx, s, tromboneHCD, startState, stateActor))
}

class DatumCommand(ctx: ActorContext[CommandMsgs],
                   s: Setup,
                   tromboneHCD: Running,
                   startState: TromboneState,
                   stateActor: Option[ActorRef[TromboneStateMsg]])
    extends MutableBehavior[CommandMsgs] {

  import csw.trombone.assembly.actors.TromboneCommandHandler._
  import csw.trombone.assembly.actors.TromboneStateActor._

  private val setStateResponseAdapter: ActorRef[StateWasSet] = ctx.spawnAdapter(SetStateResponseE)

  override def onMessage(msg: CommandMsgs): Behavior[CommandMsgs] = msg match {
    case CommandStart(replyTo) =>
      if (startState.cmd.head == cmdUninitialized) {
        replyTo ! NoLongerValid(
          WrongInternalStateIssue(s"Assembly state of ${cmd(startState)}/${move(startState)} does not allow datum")
        )
      } else {
        stateActor.foreach(
          _ ! SetState(cmdItem(cmdBusy),
                       moveItem(moveIndexing),
                       startState.sodiumLayer,
                       startState.nss,
                       setStateResponseAdapter)
        )
        tromboneHCD.hcdRef ! Submit(Setup(s.info, TromboneHcdState.axisDatumCK))
        TromboneCommandHandler.executeMatch(ctx, idleMatcher, tromboneHCD.pubSubRef, Some(replyTo)) {
          case Completed =>
            stateActor.foreach(
              _ ! SetState(cmdReady, moveIndexed, sodiumLayer = false, nss = false, setStateResponseAdapter)
            )
          case Error(message) =>
            println(s"Data command match failed with error: $message")
        }
      }
      this
    case StopCurrentCommand =>
      tromboneHCD.hcdRef ! Submit(TromboneHcdState.cancelSC(s.info))
      this

    case SetStateResponseE(response: StateWasSet) => // ignore confirmation
      this
  }
}
