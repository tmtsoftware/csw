package csw.services.location.common

import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Sink, Source, SourceQueueWithComplete}

import scala.concurrent.{Future, Promise}

object StreamExtensions {

  def coupling[T]: (Source[T, SourceQueueWithComplete[T]], Future[SourceQueueWithComplete[T]]) = {
    val source: Source[T, SourceQueueWithComplete[T]] = Source.queue[T](256, OverflowStrategy.backpressure)
    splitMat(source)
  }

  private def splitMat[T, M](src: Source[T, M]): (Source[T, M], Future[M]) = {
    val p = Promise[M]
    val s = src.mapMaterializedValue { m =>
      p.trySuccess(m)
      m
    }
    (s, p.future)
  }

}
