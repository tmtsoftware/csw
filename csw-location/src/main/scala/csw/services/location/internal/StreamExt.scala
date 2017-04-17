package csw.services.location.internal

import akka.stream.scaladsl.{Keep, Source}
import akka.stream.{KillSwitch, KillSwitches}

object StreamExt {

  /**
    * RichSource adds extra features on akka streams
    *
    * @see [[akka.stream.scaladsl.Source]]
    * @tparam Out The type of values that will flow through this stream
    * @tparam Mat When the stream starts flowing, the handle to the Mat will be available
    */
  implicit class RichSource[Out, Mat](val source: Source[Out, Mat]) extends AnyVal {

    /**
      * Gives an akka stream which can be cancelled
      */
    def cancellable: Source[Out, KillSwitch] = source.viaMat(KillSwitches.single)(Keep.right)

    /**
      * Gives an akka stream with the handle to its Mat and is also cancellable
      */
    def cancellableMat: Source[Out, (Mat, KillSwitch)] = source.viaMat(KillSwitches.single)(Keep.both)

    def distinctUntilChanged: Source[Out, Mat] = source
      .map(Option.apply)
      .prepend(Source.single(None))
      .sliding(2)
      .collect { case Seq(a, b@Some(x)) if a != b ⇒ x }
  }

}
