package csw.services.config.api.commons

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.{Future, Promise}

object ConfigStreamExts {

  implicit class RichSource[Out, Mat](val source: Source[Out, Mat]) extends AnyVal {

    def prefixAndStitch(n: Int)(implicit mat: Materializer): (Future[Seq[Out]], Source[Out, Future[NotUsed]]) = {
      import mat.executionContext
      val p = Promise[Seq[Out]]
      val futureSource = source.prefixAndTail(n).runWith(Sink.head).map {
        case (prefix, remainingSource) ⇒
          p.success(prefix)
          Source(prefix) ++ remainingSource
      }
      (p.future, Source.fromFutureSource(futureSource))
    }

  }
}
