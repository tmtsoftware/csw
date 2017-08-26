package csw.trombone.assembly.actors

import akka.typed.scaladsl.Actor
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.models.CommandMessage.Submit
import csw.trombone.assembly.TromboneControlMsg.{GoToStagePosition, UpdateTromboneHcd}
import csw.trombone.assembly.{Algorithms, AssemblyContext, TromboneControlMsg}
import csw.trombone.hcd.TromboneHcdState

object TromboneControl {
  def behavior(ac: AssemblyContext, tromboneHcd: Option[ActorRef[Submit]]): Behavior[TromboneControlMsg] =
    Actor.immutable { (ctx, msg) ⇒
      msg match {
        case GoToStagePosition(stagePosition) =>
          assert(stagePosition.units == ac.stagePositionUnits)
          val encoderPosition = Algorithms.stagePositionToEncoder(ac.controlConfig, stagePosition.head)
          assert(
            encoderPosition > ac.controlConfig.minEncoderLimit && encoderPosition < ac.controlConfig.maxEncoderLimit
          )
          tromboneHcd.foreach(
            _ ! Submit(TromboneHcdState.positionSC(ac.commandInfo, encoderPosition), ctx.spawnAnonymous(Actor.ignore))
          )
          Actor.same
        case UpdateTromboneHcd(runningIn) =>
          behavior(ac, runningIn)
      }
    }
}
