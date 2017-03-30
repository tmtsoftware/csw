package csw.services.location.internal

import akka.NotUsed
import akka.actor.ActorRef
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.{KillSwitch, KillSwitches, OverflowStrategy}

import scala.concurrent.{Future, Promise}

object StreamExt {

  /**
    * A [[akka.stream.scaladsl.Source]] with materialization of `ActorRef` is created and split to get the [[scala.concurrent.Future]]
    * of materialization.The `Future` will complete when the `Source` will be materialized but we get the handle to that
    * materialization right away to perform any operation on it. Returns a tuple of newly created `Source` that can be
    * consumed and `Future` of materialization (i.e. `ActorRef`)
    */
  def actorCoupling[T]: (Source[T, NotUsed], Future[ActorRef]) = {
    Source.actorRef[T](256, OverflowStrategy.dropHead).splitMat
  }

  /**
    * An `Extension` of [[akka.stream.scaladsl.Source]]
    *
    * @param source A `Source` for which functionality extension is required
    * @tparam Out The type of values this `Source` will contain
    * @tparam Mat The type of materialization this `Source` will materialize to
    */
  implicit class RichSource[Out, Mat](val source: Source[Out, Mat]) extends AnyVal {

    /**
      * Splits the [[akka.stream.scaladsl.Source]] to get a new `Source` that will receive values from the materialization
      * once the `Source` is materialized and the `Future` of that materialization. The `Future` will complete when the
      * `Source` will be materialized but we get the handle to that materialization right away to perform any operation
      * on it.
      */
    def splitMat: (Source[Out, NotUsed], Future[Mat]) = {
      val p = Promise[Mat]
      val s = source.mapMaterializedValue { m =>
        p.trySuccess(m)
        NotUsed
      }
      (s, p.future)
    }

    /**
      * A new `Source` is created out of the given `Source` which will be materialized to [[akka.stream.KillSwitch]].
      * This stream of `Source` can be terminated any time using the `KillSwitch`
      */
    def cancellable: Source[Out, KillSwitch] = source.viaMat(KillSwitches.single)(Keep.right)
  }

}
