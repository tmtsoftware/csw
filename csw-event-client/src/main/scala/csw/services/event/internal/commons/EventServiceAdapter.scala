package csw.services.event.internal.commons

import csw.services.event.internal.commons.javawrappers.{JEventPublisher, JEventService, JEventSubscriber}
import csw.services.event.javadsl.{IEventPublisher, IEventService, IEventSubscriber}
import csw.services.event.scaladsl.{EventPublisher, EventService, EventSubscriber}

private[csw] object EventServiceAdapter {
  def asJava(eventPublisher: EventPublisher): IEventPublisher = new JEventPublisher(eventPublisher)

  def asJava(eventSubscriber: EventSubscriber): IEventSubscriber = new JEventSubscriber(eventSubscriber)

  def asJava(eventService: EventService): IEventService = new JEventService(eventService)(eventService.executionContext)
}
