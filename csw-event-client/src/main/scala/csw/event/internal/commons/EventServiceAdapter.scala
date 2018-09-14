package csw.event.internal.commons

import csw.event.api.javadsl.{IEventPublisher, IEventService, IEventSubscriber}
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber}
import csw.event.internal.commons.javawrappers.{JEventPublisher, JEventService, JEventSubscriber}

/**
 * Adapt scala APIs of Event Service to java APIs
 */
private[csw] object EventServiceAdapter {
  def asJava(eventPublisher: EventPublisher): IEventPublisher = new JEventPublisher(eventPublisher)

  def asJava(eventSubscriber: EventSubscriber): IEventSubscriber = new JEventSubscriber(eventSubscriber)

  def asJava(eventService: EventService): IEventService = new JEventService(eventService)
}
