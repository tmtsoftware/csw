package csw.aas.core.utils

import cats.data.EitherT

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object Conversions {
  implicit class RichEitherTFuture[A, B](eitherT: EitherT[Future, A, B]) {
    def block(): Either[A, B] = Await.result(eitherT.value, 5.seconds)
  }
}
