package csw.ccs.internal.matchers

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.messages.ccs.commands.CommandResponse

import scala.concurrent.Future

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
