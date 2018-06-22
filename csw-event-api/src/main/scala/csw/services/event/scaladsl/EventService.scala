package csw.services.event.scaladsl

import scala.concurrent.{ExecutionContext, Future}

trait EventService {
  implicit val executionContext: ExecutionContext

  lazy val defaultPublisher: Future[EventPublisher]   = makeNewPublisher()
  lazy val defaultSubscriber: Future[EventSubscriber] = makeNewSubscriber()

  def makeNewPublisher(): Future[EventPublisher]
  def makeNewSubscriber(): Future[EventSubscriber]
}
