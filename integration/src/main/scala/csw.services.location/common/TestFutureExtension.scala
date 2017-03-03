package csw.services.location.common

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Future}

/**
  * Created by pritamkadam on 3/3/17.
  */
object TestFutureExtension {

  implicit class RichFuture[T](val f: Future[T]) extends AnyVal {
    def await: T = Await.result(f, 100.seconds)
  }

}
