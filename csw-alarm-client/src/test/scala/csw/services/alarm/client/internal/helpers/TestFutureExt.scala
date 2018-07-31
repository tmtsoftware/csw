package csw.services.alarm.client.internal.helpers
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, DurationDouble}

object TestFutureExt {
  implicit class RichFuture[T](f: Future[T]) {
    def await(duration: Duration): T = Await.result(f, duration)
    def await: T                     = await(10.seconds)
  }
}
