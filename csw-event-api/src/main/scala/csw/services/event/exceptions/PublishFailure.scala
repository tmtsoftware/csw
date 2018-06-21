package csw.services.event.exceptions

import csw.messages.events.Event

case class EventServerNotAvailable(cause: Throwable)      extends RuntimeException("Event Server not available")
case class PublishFailure(event: Event, cause: Throwable) extends RuntimeException(s"Publishing failed for [$event]", cause)
