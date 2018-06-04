package csw.services.event.helpers

import java.time.Instant

import csw.messages.events.{Event, EventName, EventTime, SystemEvent}
import csw.messages.params.generics.Key
import csw.messages.params.generics.KeyType.LongKey
import csw.messages.params.models.{Id, Prefix}

object Utils {
  val prefix                  = Prefix("test.prefix")
  val eventName               = EventName("system")
  val event                   = SystemEvent(prefix, eventName)
  val timeNanosKey: Key[Long] = LongKey.make("eventTime")

  def makeEvent(id: Int): Event = event.copy(
    eventId = Id(id.toString),
    eventTime = EventTime(Instant.now()),
    paramSet = Set(timeNanosKey.set(System.nanoTime()))
  )

  def makeEventWithPrefix(id: Int, prefix: Prefix): Event = event.copy(
    eventId = Id(id.toString),
    source = prefix,
    eventTime = EventTime(Instant.now()),
    paramSet = Set(timeNanosKey.set(System.nanoTime()))
  )

  def makeDistinctEvent(id: Int): Event = {
    val eventName = EventName(s"system_$id")

    SystemEvent(prefix, eventName).copy(eventId = Id(id.toString))
  }
}
