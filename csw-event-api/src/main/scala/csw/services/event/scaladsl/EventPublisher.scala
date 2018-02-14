package csw.services.event.scaladsl

import akka.Done
import csw.messages.ccs.events.Event

import scala.concurrent.Future

trait EventPublisher {

  def publish(event: Event): Future[Done]

}
