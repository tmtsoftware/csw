package romaine.extensions

import akka.stream.scaladsl.{Keep, Source}
import akka.stream.{KillSwitch, KillSwitches}

object SourceExtensions {
  implicit class RichSource[Out, Mat](val source: Source[Out, Mat]) extends AnyVal {

    def distinctUntilChanged: Source[Out, Mat] = source.statefulMapConcat { () ⇒
      var previous: Option[Out] = None
      current ⇒
        if (current == previous) List.empty
        else {
          previous = Some(current)
          List(current)
        }
    }

    def cancellable: Source[Out, KillSwitch] = source.viaMat(KillSwitches.single)(Keep.right)
  }
}
