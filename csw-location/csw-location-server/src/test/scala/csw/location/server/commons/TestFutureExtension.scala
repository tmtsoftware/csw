package csw.location.server.commons

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{Await, Future}

object TestFutureExtension {

  implicit class RichFuture[T](val f: Future[T]) extends AnyVal {
    def await: T                                     = Await.result(f, 20.seconds)
    def awaitWithTimeout(timeout: FiniteDuration): T = Await.result(f, timeout)
    def done: Future[T]                              = Await.ready(f, 20.seconds)
  }

}
