package csw.location.api.extensions

import scala.concurrent.{ExecutionContext, Future}

object EitherExtensions {

  implicit class RichEither[E <: Throwable, S](value: Future[Either[E, S]]) {
    def toFuture(implicit ec: ExecutionContext): Future[S] = value.flatMap(x => Future.fromTry(x.toTry))
  }

}
