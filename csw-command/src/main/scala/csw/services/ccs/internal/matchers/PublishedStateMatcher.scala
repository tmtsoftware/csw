package csw.services.ccs.internal.matchers

import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.typed.ActorRef
import akka.typed.scaladsl.adapter._
import csw.messages.SupervisorCommonMessage.ComponentStateSubscription
import csw.messages.models.PubSub.Subscribe
import csw.messages.params.states.CurrentState
import csw.services.ccs.internal.matchers.MatcherResponse.{MatchCompleted, MatchFailed}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

object PublishedStateMatcher {

  def ask(
      currentStateSource: ActorRef[ComponentStateSubscription],
      stateMatcher: StateMatcher
  )(implicit ec: ExecutionContext, mat: ActorMaterializer): Future[MatcherResponse] = {

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
