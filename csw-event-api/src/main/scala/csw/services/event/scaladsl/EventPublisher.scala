package csw.services.event.scaladsl

import csw.messages.ccs.events.Event
import scala.concurrent.Future

trait EventPublisher {

  def publish(event: Event): Future[Unit]

}
