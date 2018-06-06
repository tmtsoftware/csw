package csw.services.event.exceptions

import csw.messages.events.Event

case class PublishFailure(event: Event, cause: Throwable) extends RuntimeException(s"Publishing failed for [$event]", cause)
