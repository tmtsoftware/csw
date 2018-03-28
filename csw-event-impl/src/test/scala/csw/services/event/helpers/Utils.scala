package csw.services.event.helpers

import csw.messages.events.{Event, EventName, SystemEvent}
import csw.messages.params.models.{Id, Prefix}

object Utils {
  val prefix = Prefix("test.prefix")

  def makeEvent(id: Int): Event = {
    val eventName = EventName("system")

    SystemEvent(prefix, eventName).copy(eventId = Id(id.toString))
  }
  def makeDistinctEvent(id: Int): Event = {
    val eventName = EventName(s"system_$id")

    SystemEvent(prefix, eventName).copy(eventId = Id(id.toString))
  }
}
