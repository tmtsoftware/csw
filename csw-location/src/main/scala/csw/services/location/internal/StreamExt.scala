package csw.services.location.internal

import akka.stream.scaladsl.{Keep, Source}
import akka.stream.{KillSwitch, KillSwitches}

object StreamExt {
  /**
    * An `Extension` of [[akka.stream.scaladsl.Source]]
    *
    * @param source A `Source` for which functionality extension is required
    * @tparam Out The type of values this `Source` will contain
    * @tparam Mat The type of materialization this `Source` will materialize to
    */
  implicit class RichSource[Out, Mat](val source: Source[Out, Mat]) extends AnyVal {
    /**
      * A new `Source` is created out of the given `Source` which will be materialized to [[akka.stream.KillSwitch]].
      * This stream of `Source` can be terminated any time using the `KillSwitch`
      */
    def cancellable: Source[Out, KillSwitch] = source.viaMat(KillSwitches.single)(Keep.right)
    def cancellableMat: Source[Out, (Mat, KillSwitch)] = source.viaMat(KillSwitches.single)(Keep.both)

    def distinctUntilChanged: Source[Out, Mat] = source
      .map(Option.apply)
      .prepend(Source.single(None))
      .sliding(2)
      .collect { case Seq(a, b@Some(x)) if a != b ⇒ x }
  }

}
