package csw.services.event.scaladsl

trait EventService {
  val defaultPublisher: EventPublisher
  def makeNewPublisher(): EventPublisher
  val subscriber: EventSubscriber
}
