package csw.time.client.internal.extensions

import akka.actor
import csw.time.api.Cancellable
object RichCancellableExt {
  implicit class RichCancellable(val underlyingCancellable: actor.Cancellable) extends AnyVal {
    def toTsCancellable: Cancellable = () => underlyingCancellable.cancel()
  }
}
