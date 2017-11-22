package csw.services.ccs.internal.matchers

import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Sink, Source}
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter._
import csw.services.ccs.internal.matchers.MatcherResponse.{MatchCompleted, MatchFailed}
import csw.messages.SupervisorCommonMessage.ComponentStateSubscription
import csw.messages.models.PubSub.Subscribe
import csw.messages.params.states.CurrentState

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.control.NonFatal

object PublishedStateMatcher {

  def ask(currentStateSource: ActorRef[ComponentStateSubscription],
          stateMatcher: StateMatcher,
          ctx: ActorContext[_]): Future[MatcherResponse] = {

    implicit val ec: ExecutionContextExecutor = ctx.executionContext
    implicit val mat: ActorMaterializer       = ActorMaterializer()(ctx.system.toUntyped)

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
