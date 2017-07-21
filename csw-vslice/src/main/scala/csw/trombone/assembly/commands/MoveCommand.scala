package csw.trombone.assembly.commands

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.param.Parameters.Setup
import csw.param.UnitsOfMeasure.encoder
import csw.trombone.assembly._
import csw.trombone.assembly.actors.TromboneStateActor.{TromboneState, TromboneStateMsg}
import csw.common.ccs.CommandStatus.{Completed, Error, NoLongerValid}
import csw.common.ccs.Validation.WrongInternalStateIssue
import csw.common.framework.CommandMsgs
import csw.common.framework.CommandMsgs.{CommandStart, SetStateResponseE, StopCurrentCommand}
import csw.common.framework.HcdComponentLifecycleMessage.Running
import csw.common.framework.RunningHcdMsg.Submit
import csw.trombone.hcd.TromboneHcdState

object MoveCommand {
  def make(ac: AssemblyContext,
           s: Setup,
           tromboneHCD: Running,
           startState: TromboneState,
           stateActor: Option[ActorRef[TromboneStateMsg]]): Behavior[CommandMsgs] =
    Actor.mutable(ctx ⇒ new MoveCommand(ctx, ac, s, tromboneHCD, startState, stateActor))
}

class MoveCommand(ctx: ActorContext[CommandMsgs],
                  ac: AssemblyContext,
                  s: Setup,
                  tromboneHCD: Running,
                  startState: TromboneState,
                  stateActor: Option[ActorRef[TromboneStateMsg]])
    extends MutableBehavior[CommandMsgs] {

  import csw.trombone.assembly.actors.TromboneCommandHandler._
  import csw.trombone.assembly.actors.TromboneStateActor._

  private val setStateResponseAdapter: ActorRef[StateWasSet] = ctx.spawnAdapter(SetStateResponseE)

  override def onMessage(msg: CommandMsgs): Behavior[CommandMsgs] = {
    msg match {
      case CommandStart(replyTo) =>
        if (cmd(startState) == cmdUninitialized || (move(startState) != moveIndexed && move(startState) != moveMoving)) {
          replyTo ! NoLongerValid(
            WrongInternalStateIssue(s"Assembly state of ${cmd(startState)}/${move(startState)} does not allow move")
          )
        } else {
          val stagePosition   = s(ac.stagePositionKey)
          val encoderPosition = Algorithms.stagePositionToEncoder(ac.controlConfig, stagePosition.head)
          val stateMatcher    = posMatcher(encoderPosition)
          val scOut = Setup(s.info, TromboneHcdState.axisMoveCK)
            .add(TromboneHcdState.positionKey -> encoderPosition withUnits encoder)

          stateActor.foreach(
            _ !
            SetState(cmdItem(cmdBusy),
                     moveItem(moveMoving),
                     startState.sodiumLayer,
                     startState.nss,
                     setStateResponseAdapter)
          )

          tromboneHCD.hcdRef ! Submit(scOut)

          executeMatch(ctx, stateMatcher, tromboneHCD.pubSubRef, Some(replyTo)) {
            case Completed =>
              stateActor.foreach(
                _ !
                SetState(cmdItem(cmdReady),
                         moveItem(moveIndexed),
                         sodiumItem(false),
                         startState.nss,
                         setStateResponseAdapter)
              )
            case Error(message) =>
              println(s"Move command match failed with message: $message")
          }
        }

        this
      case StopCurrentCommand =>
        tromboneHCD.hcdRef ! Submit(TromboneHcdState.cancelSC(s.info))
        this
      case SetStateResponseE(_) ⇒ this
    }
  }
}
