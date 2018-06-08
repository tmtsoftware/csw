package csw.services.event.scaladsl

import scala.concurrent.Future

trait EventService {
  val defaultPublisher: Future[EventPublisher]
  val defaultSubscriber: Future[EventSubscriber]

  def makeNewPublisher(): Future[EventPublisher]
}
