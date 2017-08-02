package csw.trombone.assembly.commands

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.ccs.CommandMsgs
import csw.common.ccs.CommandMsgs.{CommandStart, SetStateResponseE, StopCurrentCommand}
import csw.common.ccs.CommandStatus.{Completed, Error, NoLongerValid}
import csw.common.ccs.Validation.WrongInternalStateIssue
import csw.common.framework.models.HcdMsg.Submit
import csw.common.framework.models.PubSub
import csw.common.framework.models.SupervisorIdleMsg.Running
import csw.param.Parameters.Setup
import csw.param.StateVariable.CurrentState
import csw.trombone.assembly.Matchers
import csw.trombone.assembly.actors.TromboneStateActor.{TromboneState, TromboneStateMsg}
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

  import csw.trombone.assembly.actors.TromboneStateActor._

  private val setStateResponseAdapter: ActorRef[StateWasSet] = ctx.spawnAdapter(SetStateResponseE)
  private val pubSubRef: ActorRef[PubSub[CurrentState]]      = ctx.system.deadLetters

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
        tromboneHCD.componentRef ! Submit(Setup(s.info, TromboneHcdState.axisDatumCK))
        Matchers.executeMatch(ctx, Matchers.idleMatcher, pubSubRef, Some(replyTo)) {
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
      tromboneHCD.componentRef ! Submit(TromboneHcdState.cancelSC(s.info))
      this

    case SetStateResponseE(response: StateWasSet) => // ignore confirmation
      this
  }
}
