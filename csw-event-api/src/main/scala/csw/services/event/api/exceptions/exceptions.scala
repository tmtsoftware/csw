package csw.services.event.api.exceptions

import csw.messages.events.Event

/**
 * An exception representing unavailability of underlying server in case of publishing or subscribing
 * @param cause Underlying exception to determine cause of server unavailability
 */
case class EventServerNotAvailable(cause: Throwable) extends RuntimeException("Event Server not available", cause)

/**
 * An exception representing any failure while publishing an Event
 * @param event The [[csw.messages.events.Event]] for which publishing failed
 * @param cause Underlying cause of failure in publishing
 */
case class PublishFailure(event: Event, cause: Throwable) extends RuntimeException(s"Publishing failed for [$event]", cause)
