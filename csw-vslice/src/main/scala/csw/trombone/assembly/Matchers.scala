package csw.trombone.assembly

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.util.Timeout
import csw.common.ccs.CommandStatus.CommandResponse
import csw.common.ccs.MultiStateMatcherMsgs.StartMatch
import csw.common.ccs._
import csw.common.framework.models.PubSub
import csw.trombone.hcd.TromboneHcdState
import akka.typed.scaladsl.AskPattern._
import csw.param.states.{CurrentState, DemandState}

import scala.concurrent.duration.DurationLong

object Matchers {

  def idleMatcher: DemandMatcher =
    DemandMatcher(
      DemandState(TromboneHcdState.axisStateCK).add(TromboneHcdState.stateKey -> TromboneHcdState.AXIS_IDLE)
    )

  def posMatcher(position: Int): DemandMatcher =
    DemandMatcher(
      DemandState(TromboneHcdState.axisStateCK)
        .madd(TromboneHcdState.stateKey -> TromboneHcdState.AXIS_IDLE, TromboneHcdState.positionKey -> position)
    )

  def executeMatch(ctx: ActorContext[_],
                   stateMatcher: StateMatcher,
                   currentStateSource: ActorRef[PubSub[CurrentState]],
                   replyTo: Option[ActorRef[CommandResponse]] = None,
                   timeout: Timeout = Timeout(5.seconds))(codeBlock: PartialFunction[CommandResponse, Unit]): Unit = {
    implicit val t                    = Timeout(timeout.duration + 1.seconds)
    implicit val scheduler: Scheduler = ctx.system.scheduler
    import ctx.executionContext

    val matcher: ActorRef[MultiStateMatcherMsgs.WaitingMsg] =
      ctx.spawnAnonymous(MultiStateMatcherActor.make(currentStateSource, timeout))
    for {
      cmdStatus <- matcher ? { x: ActorRef[CommandStatus.CommandResponse] ⇒
        StartMatch(x, stateMatcher)
      }
    } {
      codeBlock(cmdStatus)
      replyTo.foreach(_ ! cmdStatus)
    }
  }

}
