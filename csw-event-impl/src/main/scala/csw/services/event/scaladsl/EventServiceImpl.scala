package csw.services.event.scaladsl

class EventServiceImpl(eventPublisher: EventPublisher, eventSubscriber: EventSubscriber) extends EventService {
  override val defaultPublisher: EventPublisher = eventPublisher
  override val subscriber: EventSubscriber      = eventSubscriber

  override def makeNewPublisher(): EventPublisher = ???
}
