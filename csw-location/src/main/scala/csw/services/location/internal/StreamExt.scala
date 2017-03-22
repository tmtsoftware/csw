package csw.services.location.internal

import akka.NotUsed
import akka.actor.ActorRef
import akka.stream.scaladsl.{BroadcastHub, Keep, Sink, Source, SourceQueueWithComplete}
import akka.stream.{KillSwitch, KillSwitches, Materializer, OverflowStrategy}

import scala.concurrent.{Future, Promise}

object StreamExt {

  def coupling[T](implicit mat: Materializer): (SourceQueueWithComplete[T], Source[T, KillSwitch]) = {
    Source
      .queue[T](8, OverflowStrategy.dropHead)
      .toMat(broadcastSink[T](8, OverflowStrategy.dropHead))(Keep.both).run()
  }

  def actorCoupling[T]: (Source[T, NotUsed], Future[ActorRef]) = {
    Source.actorRef[T](256, OverflowStrategy.dropHead).splitMat
  }

  def broadcastSink[T](
    bufferSize: Int, overflowStrategy: OverflowStrategy
  )(implicit mat: Materializer): Sink[T, Source[T, KillSwitch]] = {

    BroadcastHub.sink[T](bufferSize / 2).mapMaterializedValue { hub =>
      val cancellableHub = hub
        .viaMat(KillSwitches.single)(Keep.right)
        //buffering is only required to define custom overflow strategy
        //it will go away if BroadcastHub.sink is not hardcoded to backpressure
        .buffer(bufferSize / 2, overflowStrategy)

      // Ensure that the Broadcast output is dropped if there are no listening parties.
      cancellableHub.runWith(Sink.ignore)
      cancellableHub
    }
  }

  implicit class RichSource[Out, Mat](val source: Source[Out, Mat]) extends AnyVal {

    def broadcast()(implicit mat: Materializer): Source[Out, KillSwitch] =
      source.runWith(broadcastSink[Out](16, OverflowStrategy.dropHead))

    def splitMat: (Source[Out, NotUsed], Future[Mat]) = {
      val p = Promise[Mat]
      val s = source.mapMaterializedValue { m =>
        p.trySuccess(m)
        NotUsed
      }
      (s, p.future)
    }

    def cancellable: Source[Out, KillSwitch] = source.viaMat(KillSwitches.single)(Keep.right)
  }

}
