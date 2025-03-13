/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package romaine.extensions

import org.apache.pekko.stream.scaladsl.{Keep, Source}
import org.apache.pekko.stream.{KillSwitch, KillSwitches}

object SourceExtensions {
  implicit class RichSource[Out, Mat](val source: Source[Out, Mat]) extends AnyVal {

//    XXX statefulMapConcat is Deprecated
//    def distinctUntilChanged: Source[Out, Mat] =
//      source.statefulMapConcat { () =>
//        var previous: Option[Out] = None
//        current =>
//          if (previous.contains(current)) List.empty
//          else {
//            previous = Some(current)
//            List(current)
//          }
//      }

    def distinctUntilChanged: Source[Out, Mat] =
      source
        .statefulMap(() => List.empty[Out])(
          (state, elem) => {
            if (state.contains(elem))
              (state, Nil)
            else
              (List(elem), List(elem))
          },
          state => None
        )
        .mapConcat(identity)

    def cancellable: Source[Out, KillSwitch] = source.viaMat(KillSwitches.single)(Keep.right)
  }
}
