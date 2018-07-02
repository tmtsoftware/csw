package csw.services.event.internal.commons

import akka.stream.Supervision
import csw.services.event.exceptions.EventServerNotAvailable

/**
 * Use this decider for Event streams to stop the stream in case underlying server is not available and resume
 * in all other cases of exceptions
 */
object EventStreamSupervisionStrategy {
  val decider: Supervision.Decider = {
    case _: EventServerNotAvailable ⇒ Supervision.Stop
    case _                          ⇒ Supervision.Resume
  }
}
