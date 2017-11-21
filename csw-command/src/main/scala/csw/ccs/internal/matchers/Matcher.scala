package csw.ccs.internal.matchers

import akka.stream.ActorMaterializer
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter._
import csw.messages.ccs.commands.CommandResponse

import scala.concurrent.{ExecutionContextExecutor, Future}

abstract class Matcher[T](ctx: ActorContext[_]) {
  implicit val ec: ExecutionContextExecutor = ctx.executionContext
  implicit val mat: ActorMaterializer       = ActorMaterializer()(ctx.system.toUntyped)

  def executeMatch(transformResponse: T ⇒ CommandResponse): Future[CommandResponse]
}
