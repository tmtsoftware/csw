package csw.time.client.internal

import java.time.Instant

import csw.time.api.models.{CswInstant, TimeScale, TimeScales}
import csw.time.api.scaladsl.TimeService
import csw.time.client.internal.native_models.{NTPTimeVal, TimeSpec}

class TimeServiceImpl() extends TimeService {
  val ClockRealtime = 0
  val ClockTAI      = 11

  override def UTCTime(): CswInstant = time(TimeScales.UTCScale)

  override def TAITime(): CswInstant = time(TimeScales.TAIScale)

  override def TAIOffset(): Int = {
    val timeVal = new NTPTimeVal()
    TimeLibrary.ntp_gettimex(timeVal)
    timeVal.tai
  }

  private def time(scale: TimeScale): CswInstant = {
    val clockId = scale match {
      case TimeScales.UTCScale => ClockRealtime
      case TimeScales.TAIScale => ClockTAI
    }

    val timeSpec = new TimeSpec()
    TimeLibrary.clock_gettime(clockId, timeSpec)

    val instant = Instant.ofEpochSecond(timeSpec.seconds, timeSpec.nanoseconds)
    CswInstant(instant, scale)
  }

}
