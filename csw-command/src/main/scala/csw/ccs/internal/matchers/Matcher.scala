package csw.ccs.internal.matchers

import akka.stream.ActorMaterializer
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter._

import scala.concurrent.ExecutionContextExecutor

abstract class Matcher(ctx: ActorContext[_]) {
  implicit val ec: ExecutionContextExecutor = ctx.executionContext
  implicit val mat: ActorMaterializer       = ActorMaterializer()(ctx.system.toUntyped)

}
