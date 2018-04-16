package csw.services.event.exceptions

import csw.messages.events.Event

case class PublishFailed(event: Event, message: String)
    extends RuntimeException(s"Publishing failed for [$event] due to [$message]")
