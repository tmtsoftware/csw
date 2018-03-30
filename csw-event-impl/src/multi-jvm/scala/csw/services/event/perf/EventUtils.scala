package csw.services.event.perf

import csw.messages.events.{Event, EventKey, EventName, SystemEvent}
import csw.messages.params.generics.Key
import csw.messages.params.generics.KeyType.{ByteKey, LongKey, StringKey}
import csw.messages.params.models.{Id, Prefix}

object EventUtils {
  val prefix           = Prefix("tcs.mobie.filter")
  val testEvent        = "move"
  val eventKey         = s"${prefix.prefix}.$testEvent"
  val warmupEvent      = EventName("warmup")
  val startEvent       = EventName("start")
  val endEvent         = EventName("end")
  val flowControlEvent = EventName("flowcontrol")

  val flowctlKey: Key[Long]     = LongKey.make("flowctl")
  val byteKey: Key[Byte]        = ByteKey.make("byteKey")
  val publisherKey: Key[String] = StringKey.make("pubKey")

  val eventKeys: Set[EventKey] =
    Set(
      EventKey(s"${prefix.prefix}.$warmupEvent"),
      EventKey(s"${prefix.prefix}.$flowControlEvent"),
      EventKey(s"${prefix.prefix}.$startEvent"),
      EventKey(s"${prefix.prefix}.$endEvent")
    )

  def makeEvent(name: EventName, id: Long = -1, payload: Array[Byte] = Array.emptyByteArray): Event =
    SystemEvent(prefix, name).copy(eventId = Id(id.toString), paramSet = Set(byteKey.set(payload)))

  def makeFlowCtlEvent(id: Int, time: Long, name: String): Event =
    SystemEvent(prefix, flowControlEvent).copy(eventId = Id(id.toString), paramSet = Set(flowctlKey.set(time)))

}
