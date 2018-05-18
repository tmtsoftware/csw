package csw.services.event.perf.utils

import java.time.Instant

import csw.messages.events.{EventKey, EventName, EventTime, SystemEvent}
import csw.messages.params.generics.Key
import csw.messages.params.generics.KeyType.{ByteKey, DoubleKey, LongKey}
import csw.messages.params.models.{Id, Prefix}

object EventUtils {
  val prefix       = Prefix("tcs.mobie.filter")
  val testEventS   = "move"
  val testEventKey = s"${prefix.prefix}.$testEventS"
  val endEventS    = "end"

  val payloadKey: Key[Byte]         = ByteKey.make("payloadKey")
  val histogramKey: Key[Byte]       = ByteKey.make("histogramKey")
  val throughputKey: Key[Double]    = DoubleKey.make("throughputKey")
  val totalDroppedKey: Key[Long]    = LongKey.make("totalDroppedKey")
  val totalOutOfOrderKey: Key[Long] = LongKey.make("totalOutOfOrderKey")

  val baseTestEvent = SystemEvent(prefix, EventName(testEventS))

  val basePerfEvent          = SystemEvent(prefix, EventName("perf"))
  val perfEventKey: EventKey = basePerfEvent.eventKey

  def perfResultEvent(payload: Array[Byte], throughput: Double, totalDropped: Long, totalOutOfOrder: Long): SystemEvent =
    basePerfEvent.copy(
      paramSet = Set(histogramKey.set(payload),
                     throughputKey.set(throughput),
                     totalDroppedKey.set(totalDropped),
                     totalOutOfOrderKey.set(totalOutOfOrder))
    )

  def event(name: EventName, id: Long = -1, payload: Array[Byte] = Array.emptyByteArray): SystemEvent =
    baseTestEvent.copy(
      eventId = Id(id.toString),
      eventName = name,
      paramSet = Set(payloadKey.set(payload)),
      eventTime = EventTime()
    )

  def nanosToMicros(nanos: Double): Double  = nanos / Math.pow(10, 3)
  def nanosToMillis(nanos: Double): Double  = nanos / Math.pow(10, 6)
  def nanosToSeconds(nanos: Double): Double = nanos / Math.pow(10, 9)
  def getNanos(instant: Instant): Double    = instant.getEpochSecond * Math.pow(10, 9) + instant.getNano

}
