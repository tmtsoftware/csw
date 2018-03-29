package csw.services.event.perf

import csw.messages.events.{Event, EventKey, EventName, SystemEvent}
import csw.messages.params.generics.Key
import csw.messages.params.generics.KeyType.{ByteArrayKey, ByteKey, LongKey}
import csw.messages.params.models.{ArrayData, Id, Prefix}

object EventUtils {
  val prefix               = Prefix("tcs.mobie.filter")
  val eventName            = EventName("filter")
  val warmupEventName      = EventName("warmup")
  val startEventName       = EventName("start")
  val endEventName         = EventName("end")
  val flowControlEventName = EventName("flowcontrol")

  private val flowctlKey: Key[Long]   = LongKey.make("flowctl")
  private val byteArrayKey: Key[Byte] = ByteKey.make("byteKey")

  val eventKeys: Set[EventKey] =
    Set(
      EventKey(s"${prefix.prefix}.$eventName"),
      EventKey(s"${prefix.prefix}.$warmupEventName"),
      EventKey(s"${prefix.prefix}.$flowControlEventName"),
      EventKey(s"${prefix.prefix}.$startEventName"),
      EventKey(s"${prefix.prefix}.$endEventName")
    )

  def makeEvent(name: EventName, id: Long = -1, payload: Array[Byte] = Array.emptyByteArray): Event =
    SystemEvent(prefix, name).copy(eventId = Id(id.toString), paramSet = Set(byteArrayKey.set(payload)))

  def makeFlowCtlEvent(id: Int, time: Long): Event =
    SystemEvent(prefix, flowControlEventName).copy(eventId = Id(id.toString), paramSet = Set(flowctlKey.set(time)))

}
