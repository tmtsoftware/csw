package csw.services.location.impl

import akka.stream.scaladsl.{BroadcastHub, Keep, Sink, Source, SourceQueueWithComplete}
import akka.stream.{KillSwitch, KillSwitches, Materializer, OverflowStrategy}

object StreamExt {

  def coupling[T](implicit mat: Materializer): (SourceQueueWithComplete[T], Source[T, KillSwitch]) = {
    Source
      .queue[T](8, OverflowStrategy.dropHead)
      .toMat(broadcastSink[T](8, OverflowStrategy.dropHead))(Keep.both).run()
  }

  def broadcastSink[T](
    bufferSize: Int, overflowStrategy: OverflowStrategy
  )(implicit mat: Materializer): Sink[T, Source[T, KillSwitch]] = {

    BroadcastHub.sink[T](bufferSize / 2).mapMaterializedValue { hub =>
      val cancellableHub = hub
        .viaMat(KillSwitches.single)(Keep.right)
        .buffer(bufferSize / 2, overflowStrategy)

      // Ensure that the Broadcast output is dropped if there are no listening parties.
      cancellableHub.runWith(Sink.ignore)
      cancellableHub
    }
  }

  implicit class RichSource[Out, Mat](val source: Source[Out, Mat]) extends AnyVal {

    def broadcast()(implicit mat: Materializer): Source[Out, KillSwitch] =
      source.runWith(broadcastSink[Out](16, OverflowStrategy.dropHead))
  }

}
