package csw.services.event.scaladsl

import akka.Done

import scala.concurrent.Future

trait EventSubscription {
  def unsubscribe(): Future[Done]
}
