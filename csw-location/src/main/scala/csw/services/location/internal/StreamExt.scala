package csw.services.location.internal

import akka.NotUsed
import akka.actor.ActorRef
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.{KillSwitch, KillSwitches, OverflowStrategy}

import scala.concurrent.{Future, Promise}

object StreamExt {

  def actorCoupling[T]: (Source[T, NotUsed], Future[ActorRef]) = {
    Source.actorRef[T](256, OverflowStrategy.dropHead).splitMat
  }

  implicit class RichSource[Out, Mat](val source: Source[Out, Mat]) extends AnyVal {

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
