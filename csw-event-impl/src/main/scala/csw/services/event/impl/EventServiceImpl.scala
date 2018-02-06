package csw.services.event.impl

import csw.messages.ccs.events.{Event, EventKey}
import csw.services.event.scaladsl.{EventService, EventServiceDriver}
import csw_protobuf.events.PbEvent

class EventServiceImpl(eventServiceDriver: EventServiceDriver) extends EventService {

  override def publish(event: Event): Unit = {

    val pbEvent: PbEvent = Event.typeMapper.toBase(event)

    eventServiceDriver.publish(event.eventKey.toString, pbEvent)
  }

  override def subscribeEvent(eventKey: EventKey): Unit = ???
}
