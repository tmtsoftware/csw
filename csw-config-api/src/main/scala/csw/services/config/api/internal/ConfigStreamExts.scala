package csw.services.config.api.internal

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.{Future, Promise}

/**
 * Extensions on akka streams
 */
object ConfigStreamExts {

  /**
   * RichSource adds extra features on akka streams
   *
   * @see [[akka.stream.scaladsl.Source]]
   * @tparam Out The type of values that will flow through this stream
   * @tparam Mat When the stream starts flowing, the handle to the Mat will be available
   */
  implicit class RichSource[Out, Mat](val source: Source[Out, Mat]) extends AnyVal {

    /**
     * Takes up to `n` elements from the stream (less than `n` only if the upstream completes before emitting `n`
     * elements) and returns a pair containing a strict sequence of the taken element and the original stream
     * @param n number of elements to be extracted as prefix
     */
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
