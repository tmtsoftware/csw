package csw.services.event.scaladsl

trait EventService {
  val defaultPublisher: EventPublisher
  val defaultSubscriber: EventSubscriber

  def makeNewPublisher(): EventPublisher
}
