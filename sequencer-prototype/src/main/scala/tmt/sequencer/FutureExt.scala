package tmt.sequencer

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object FutureExt {
  implicit class RichFuture[T](val f: Future[T]) extends AnyVal {
    def await(duration: Duration): T = Await.result(f, duration)
    def await: T                     = await(Duration.Inf)
  }
}
