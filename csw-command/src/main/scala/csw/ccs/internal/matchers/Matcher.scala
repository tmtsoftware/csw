package csw.ccs.internal.matchers

import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter._
import akka.util.Timeout
import csw.ccs.internal.matchers.MatcherResponse.{MatchCompleted, MatchFailed}
import csw.messages.PubSub.Subscribe
import csw.messages.SupervisorCommonMessage.ComponentStateSubscription
import csw.messages.params.states.CurrentState

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.util.control.NonFatal

object Matcher {

  def matchState(
      ctx: ActorContext[_],
      stateMatcher: StateMatcher,
      currentStateSource: ActorRef[ComponentStateSubscription],
      timeout: Timeout = Timeout(5.seconds)
  ): Future[MatcherResponse] = {

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
      .map(_ ⇒ MatchCompleted)
      .recover {
        case NonFatal(ex) ⇒ MatchFailed(ex)
      }
  }
}
