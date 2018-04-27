package csw.services.event.internal.wiring

import csw.messages.events.Event

case class FailedEvent(event: Event, throwable: Throwable)
