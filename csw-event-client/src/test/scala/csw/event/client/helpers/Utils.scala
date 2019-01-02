package csw.event.client.helpers

import csw.params.core.generics.KeyType.{IntKey, LongKey}
import csw.params.core.generics.{Key, Parameter}
import csw.params.core.models.{Id, Prefix}
import csw.params.events.{Event, EventName, EventTime, SystemEvent}
import csw.params.javadsl.JKeyType
import csw.time.api.models.UTCTime

object Utils {
  val prefix                  = Prefix("test.prefix")
  val eventName               = EventName("system")
  val event                   = SystemEvent(prefix, eventName)
  val timeNanosKey: Key[Long] = LongKey.make("eventTime")

  private val jParam: Parameter[Integer] = JKeyType.IntKey.make("counter").set(1)
  private val param: Parameter[Int]      = IntKey.make("counter").set(1)

  def makeEvent(id: Int): Event = event.copy(
    eventId = Id(id.toString),
    eventTime = EventTime(UTCTime.now()),
    paramSet = Set(timeNanosKey.set(System.nanoTime()))
  )

  def makeEventForKeyName(name: EventName, id: Int): Event = event.copy(eventName = name, eventId = Id(id.toString))

  def makeEventForPrefixAndKeyName(prefix: Prefix, name: EventName, id: Int): Event =
    SystemEvent(prefix, name).copy(eventId = Id(id.toString))

  def makeEventWithPrefix(id: Int, prefix: Prefix): Event = event.copy(
    eventId = Id(id.toString),
    source = prefix,
    eventTime = EventTime(UTCTime.now()),
    paramSet = Set(timeNanosKey.set(System.nanoTime()))
  )

  def makeDistinctEvent(id: Int): Event = {
    val eventName = EventName(s"system_$id")

    SystemEvent(prefix, eventName).copy(eventId = Id(id.toString), paramSet = Set(param))
  }

  def makeDistinctJavaEvent(id: Int): Event = {
    val eventName = EventName(s"system_$id")

    SystemEvent(prefix, eventName).copy(eventId = Id(id.toString), paramSet = Set(jParam))
  }
}
