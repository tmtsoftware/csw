package csw.trombone.assembly

import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter._
import akka.util.Timeout
import csw.ccs._
import csw.messages.CommandExecutionResponse
import csw.messages.CommandExecutionResponse.{Completed, Error}
import csw.messages.PubSub.Subscribe
import csw.messages.SupervisorCommonMessage.ComponentStateSubscription
import csw.messages.params.states.{CurrentState, DemandState}
import csw.trombone.hcd.TromboneHcdState

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.util.control.NonFatal

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

  def matchState(ctx: ActorContext[_],
                 stateMatcher: StateMatcher,
                 currentStateSource: ActorRef[ComponentStateSubscription],
                 timeout: Timeout = Timeout(5.seconds)): Future[CommandExecutionResponse] = {

    import ctx.executionContext
    implicit val mat: ActorMaterializer = ActorMaterializer()(ctx.system.toUntyped)

    val source = Source
      .actorRef[CurrentState](256, OverflowStrategy.fail)
      .mapMaterializedValue { ref ⇒
        currentStateSource ! ComponentStateSubscription(Subscribe(ref))
      }
      .filter(cs ⇒ cs.prefixStr == stateMatcher.prefix && stateMatcher.check(cs))
      .completionTimeout(timeout.duration)

    source
      .runWith(Sink.head)
      .map(_ ⇒ Completed())
      .recover {
        case NonFatal(ex) ⇒ Error("")
      }
  }
}
