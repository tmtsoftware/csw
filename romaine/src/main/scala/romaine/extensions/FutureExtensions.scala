package romaine.extensions

import akka.Done
import romaine.exceptions.RedisOperationFailed

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

object FutureExtensions {
  implicit class RichFuture(response: Future[String]) {
    def failWith(reason: ⇒ String)(implicit ec: ExecutionContext): Future[Done] =
      response
        .map {
          case "OK" | "QUEUED" ⇒ Done
          case _               ⇒ throw RedisOperationFailed(reason)
        }
        .recoverWith {
          case NonFatal(ex) ⇒ throw RedisOperationFailed(reason, ex)
        }
  }
}
