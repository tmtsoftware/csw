package csw.services.event.internal.commons

import akka.stream.Supervision
import csw.services.event.exceptions.EventServerNotAvailable

object EventStreamSupervisionStrategy {
  val decider: Supervision.Decider = {
    case _: EventServerNotAvailable ⇒ Supervision.Stop
    case _                          ⇒ Supervision.Resume
  }
}
