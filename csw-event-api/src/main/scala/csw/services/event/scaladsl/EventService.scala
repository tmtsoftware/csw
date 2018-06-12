package csw.services.event.scaladsl

import scala.concurrent.{ExecutionContext, Future}

trait EventService {
  implicit val executionContext: ExecutionContext
  val defaultPublisher: Future[EventPublisher]
  val defaultSubscriber: Future[EventSubscriber]

  def makeNewPublisher(): Future[EventPublisher]
}
