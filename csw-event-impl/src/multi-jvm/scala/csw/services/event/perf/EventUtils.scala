package csw.services.event.perf

import csw.messages.events.{Event, EventKey, EventName, SystemEvent}
import csw.messages.params.generics.Key
import csw.messages.params.generics.KeyType.{ByteKey, LongKey, StringKey}
import csw.messages.params.models.{Id, Prefix}

object EventUtils {
  val prefix           = Prefix("tcs.mobie.filter")
  val testEvent        = "move"
  val testEventKey     = s"${prefix.prefix}.$testEvent"
  val warmupEvent      = EventName("warmup")
  val startEvent       = EventName("start")
  val endEvent         = EventName("end")
  val flowControlEvent = EventName("flowcontrol")

  val flowCtlKey: Key[Long]     = LongKey.make("flowCtlKey")
  val payloadKey: Key[Byte]     = ByteKey.make("payloadKey")
  val publisherKey: Key[String] = StringKey.make("pubKey")

  val baseFlowControlEvent = SystemEvent(prefix, flowControlEvent)
  val baseTestEvent        = SystemEvent(prefix, EventName(testEvent))

  val eventKeys: Set[EventKey] =
    Set(
      EventKey(s"${prefix.prefix}.$warmupEvent"),
      EventKey(s"${prefix.prefix}.$flowControlEvent"),
      EventKey(s"${prefix.prefix}.$startEvent"),
      EventKey(s"${prefix.prefix}.$endEvent")
    )

  def event(name: EventName, id: Long = -1, payload: Array[Byte] = Array.emptyByteArray): Event = {
    baseTestEvent.copy(eventId = Id(id.toString), eventName = name, paramSet = Set(payloadKey.set(payload)))
  }

  def flowCtlEvent(id: Int, time: Long, name: String): Event = {
    baseFlowControlEvent
      .copy(
        eventId = Id(id.toString),
        paramSet = Set(flowCtlKey.set(time), publisherKey.set(name))
      )
  }

}
