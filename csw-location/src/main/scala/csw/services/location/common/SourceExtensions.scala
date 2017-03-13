package csw.services.location.common

import akka.NotUsed
import akka.stream.{KillSwitch, KillSwitches, Materializer, OverflowStrategy}
import akka.stream.scaladsl.{BroadcastHub, Keep, Sink, Source, SourceQueueWithComplete}

import scala.concurrent.{Future, Promise}

object SourceExtensions {

  def coupling[T]: (Source[T, NotUsed], Future[SourceQueueWithComplete[T]]) = {
    Source.queue[T](256, OverflowStrategy.dropHead).splitMat
  }

  implicit class RichSource[Out, Mat](val source: Source[Out, Mat]) extends AnyVal {

    def broadcast(
      bufferSize: Int = 256,
      overflowStrategy: OverflowStrategy = OverflowStrategy.dropHead
    )(implicit mat: Materializer): Source[Out, KillSwitch] = {
      val hub = source
        .runWith(BroadcastHub.sink[Out](bufferSize/2))
        .viaMat(KillSwitches.single)(Keep.right)
        .buffer(bufferSize/2, overflowStrategy)

      // Ensure that the Broadcast output is dropped if there are no listening parties.
      hub.runWith(Sink.ignore)
      hub
    }

    def splitMat: (Source[Out, NotUsed], Future[Mat]) = {
      val p = Promise[Mat]
      val s = source.mapMaterializedValue { m =>
        p.trySuccess(m)
        NotUsed
      }
      (s, p.future)
    }
  }
}
