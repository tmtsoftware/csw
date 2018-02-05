package csw.services.event.impl

import csw.messages.ccs.events.Event
import csw.services.event.scaladsl.{EventService, EventServiceDriver}
import csw.services.models.EventChannel
import csw_protobuf.events.PbEvent

class EventServiceImpl(eventServiceDriver: EventServiceDriver) extends EventService {

  override def publishEvent(event: Event): Unit = {
    val eventChannel: String = EventChannel(event).channel
    val pbEvent: PbEvent     = Event.typeMapper.toBase(event)

    eventServiceDriver.publishToChannel(pbEvent, eventChannel)
  }

}
