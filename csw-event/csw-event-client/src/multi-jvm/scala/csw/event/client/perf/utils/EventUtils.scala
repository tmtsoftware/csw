/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.client.perf.utils

import java.time.Instant

import csw.params.core.generics.Key
import csw.params.core.generics.KeyType.{ByteKey, DoubleKey, LongKey}
import csw.params.core.models.Id
import csw.params.events.{EventKey, EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime

object EventUtils {
  private val prefix = Prefix("csw.dummy")
  val testEventS     = "move"
  val endEventS      = "end"

  val payloadKey: Key[Byte]                 = ByteKey.make("payloadKey")
  val histogramKey: Key[Byte]               = ByteKey.make("histogramKey")
  val initialLatencyHistogramKey: Key[Byte] = ByteKey.make("initialLatencyHistogramKey")
  val throughputKey: Key[Double]            = DoubleKey.make("throughputKey")
  val totalDroppedKey: Key[Long]            = LongKey.make("totalDroppedKey")
  val totalOutOfOrderKey: Key[Long]         = LongKey.make("totalOutOfOrderKey")
  val avgLatencyKey: Key[Long]              = LongKey.make("avgLatencyKey")

  val baseTestEvent          = SystemEvent(prefix, EventName(testEventS))
  val basePerfEvent          = SystemEvent(prefix, EventName("perf"))
  val perfEventKey: EventKey = basePerfEvent.eventKey

  def perfResultEvent(
      payload: Array[Byte],
      initialLatencyPayload: Array[Byte],
      throughput: Double,
      totalDropped: Long,
      totalOutOfOrder: Long,
      avgLatency: Long
  ): SystemEvent =
    basePerfEvent.copy(
      paramSet = Set(
        histogramKey.setAll(payload),
        initialLatencyHistogramKey.setAll(initialLatencyPayload),
        throughputKey.set(throughput),
        totalDroppedKey.set(totalDropped),
        totalOutOfOrderKey.set(totalOutOfOrder),
        avgLatencyKey.set(avgLatency)
      )
    )

  def event(name: EventName, prefix: Prefix, id: Long = -1, payload: Array[Byte] = Array.emptyByteArray): SystemEvent =
    baseTestEvent.copy(
      eventId = Id(id.toString),
      source = prefix,
      eventName = name,
      paramSet = Set(payloadKey.setAll(payload)),
      eventTime = UTCTime.now()
    )

  def nanosToMicros(nanos: Double): Double  = nanos / Math.pow(10, 3)
  def nanosToMillis(nanos: Double): Double  = nanos / Math.pow(10, 6)
  def nanosToSeconds(nanos: Double): Double = nanos / Math.pow(10, 9)
  def getNanos(instant: Instant): Double    = instant.getEpochSecond * Math.pow(10, 9) + instant.getNano

}
