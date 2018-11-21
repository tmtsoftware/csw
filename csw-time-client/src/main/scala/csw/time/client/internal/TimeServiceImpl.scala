package csw.time.client.internal

import java.time.Instant

import csw.time.api.models.CswInstant.{TaiInstant, UtcInstant}
import csw.time.api.scaladsl.TimeService
import csw.time.client.internal.native_models.{NTPTimeVal, TimeSpec}

class TimeServiceImpl() extends TimeService {
  val ClockRealtime = 0
  val ClockTAI      = 11

  override def UtcTime(): UtcInstant = UtcInstant(instantFor(ClockRealtime))

  override def TaiTime(): TaiInstant = TaiInstant(instantFor(ClockTAI))

  override def TaiOffset(): Int = {
    val timeVal = new NTPTimeVal()
    TimeLibrary.ntp_gettimex(timeVal)
    timeVal.tai
  }

  private def instantFor(clockId: Int): Instant = {
    val timeSpec = new TimeSpec()
    TimeLibrary.clock_gettime(clockId, timeSpec)

    Instant.ofEpochSecond(timeSpec.seconds, timeSpec.nanoseconds)
  }

}
