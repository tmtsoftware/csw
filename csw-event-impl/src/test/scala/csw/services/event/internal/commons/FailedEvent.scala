package csw.services.event.internal.commons

import csw.messages.events.Event

case class FailedEvent(event: Event, throwable: Throwable)
