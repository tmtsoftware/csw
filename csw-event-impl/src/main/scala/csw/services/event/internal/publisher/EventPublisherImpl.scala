package csw.services.event.internal.publisher

import csw.messages.ccs.events.Event
import csw.services.event.scaladsl.{EventPublisher, EventServiceDriver}
import csw_protobuf.events.PbEvent

import scala.concurrent.Future

class EventPublisherImpl(eventServiceDriver: EventServiceDriver) extends EventPublisher {

  override def publish(event: Event): Future[Unit] = {

    val pbEvent: PbEvent = Event.typeMapper.toBase(event)

    eventServiceDriver.publish(event.eventKey.toString, pbEvent)

  }

}
