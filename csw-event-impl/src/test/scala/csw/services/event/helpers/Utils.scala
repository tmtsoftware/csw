package csw.services.event.helpers

import csw.messages.ccs.events.{Event, EventName, SystemEvent}
import csw.messages.params.models.{Id, Prefix}

object Utils {
  def makeEvent(id: Int): Event = {
    val prefix    = Prefix("test.prefix")
    val eventName = EventName("system")

    SystemEvent(prefix, eventName).copy(eventId = Id(id.toString))
  }
}
