package csw.ccs.internal.matchers

import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Sink, Source}
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter._
import csw.ccs.internal.matchers.MatcherResponse.{MatchCompleted, MatchFailed}
import csw.messages.SupervisorCommonMessage.ComponentStateSubscription
import csw.messages.ccs.commands.CommandResponse
import csw.messages.models.PubSub.Subscribe
import csw.messages.params.states.CurrentState

import scala.concurrent.Future
import scala.util.control.NonFatal

class PublishedStateMatcher(
    ctx: ActorContext[_],
    currentStateSource: ActorRef[ComponentStateSubscription],
    stateMatcher: StateMatcher
) extends Matcher[MatcherResponse](ctx) {

  def executeMatch(transformResponse: MatcherResponse ⇒ CommandResponse): Future[CommandResponse] =
    matchState(stateMatcher, currentStateSource).map(transformResponse)

  private def matchState(
      stateMatcher: StateMatcher,
      currentStateSource: ActorRef[ComponentStateSubscription]
  ): Future[MatcherResponse] = {
    val source = Source
      .actorRef[CurrentState](256, OverflowStrategy.fail)
      .mapMaterializedValue { ref ⇒
        currentStateSource ! ComponentStateSubscription(Subscribe(ref))
      }
      .filter(cs ⇒ cs.prefixStr == stateMatcher.prefix && stateMatcher.check(cs))
      .completionTimeout(stateMatcher.timeout.duration)

    source
      .runWith(Sink.head)
      .map(_ ⇒ MatchCompleted)
      .recover {
        case NonFatal(ex) ⇒ MatchFailed(ex)
      }
  }

}
