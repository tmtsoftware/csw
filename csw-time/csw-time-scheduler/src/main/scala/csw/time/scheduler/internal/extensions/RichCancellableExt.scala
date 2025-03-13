/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.time.scheduler.internal.extensions

import org.apache.pekko.actor
import csw.time.scheduler.api.Cancellable

private[time] object RichCancellableExt {
  implicit class RichCancellable(val underlyingCancellable: actor.Cancellable) extends AnyVal {
    def toTsCancellable: Cancellable = () => underlyingCancellable.cancel()
  }
}
