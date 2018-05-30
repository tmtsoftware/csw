package csw.services.event.internal.commons

import csw.services.event.internal.pubsub.{JEventPublisher, JEventSubscriber}
import csw.services.event.javadsl.{IEventPublisher, IEventSubscriber}
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber}

object EventServiceAdapter {
  def asJava(eventPublisher: EventPublisher): IEventPublisher =
    new JEventPublisher(eventPublisher)

  def asJava(eventSubscriber: EventSubscriber): IEventSubscriber =
    new JEventSubscriber(eventSubscriber)
}
