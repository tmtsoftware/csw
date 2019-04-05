package csw.time.scheduler.internal.extensions

import akka.actor
import csw.time.scheduler.api.Cancellable

private[time] object RichCancellableExt {
  implicit class RichCancellable(val underlyingCancellable: actor.Cancellable) extends AnyVal {
    def toTsCancellable: Cancellable = () => underlyingCancellable.cancel()
  }
}
