package csw.ccs.internal.matchers

import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter._
import akka.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.ccs.internal.matchers.MatcherResponse.{MatchCompleted, MatchFailed}
import csw.messages.PubSub.Subscribe
import csw.messages.SupervisorCommonMessage.ComponentStateSubscription
import csw.messages.ccs.commands.CommandResponse
import csw.messages.params.states.CurrentState

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.control.NonFatal

abstract class Matcher(ctx: ActorContext[_]) {
  implicit val ec: ExecutionContextExecutor = ctx.executionContext
  implicit val mat: ActorMaterializer       = ActorMaterializer()(ctx.system.toUntyped)

}
class PublishedStateMatcher(
    ctx: ActorContext[_],
    currentStateSource: ActorRef[ComponentStateSubscription],
    stateMatcher: StateMatcher
) extends Matcher(ctx) {

  def executeMatch(partialFunction: PartialFunction[MatcherResponse, CommandResponse]): Future[CommandResponse] =
    matchState(stateMatcher, currentStateSource).map(partialFunction)

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

class ResponseMatcher[T](
    ctx: ActorContext[_],
    destination: ActorRef[T],
    command: T,
    timeout: Timeout
) extends Matcher(ctx) {

  def executeMatch(partialFunction: PartialFunction[CommandResponse, CommandResponse]): Future[CommandResponse] =
    (destination ? execute(command))(timeout, ctx.system.scheduler).map(partialFunction)

  private def execute(x: T)(replyTo: ActorRef[CommandResponse]): T = x

}
