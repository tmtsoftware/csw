package csw.services.event.exceptions

import csw.messages.events.Event

case class PublishFailedException(event: Event) extends RuntimeException(s"Publishing failed for [$event]")
