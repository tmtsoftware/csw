package csw.time.client

import java.time.{Clock, Instant}

import csw.time.api._

class TimeServiceImpl(clock: Clock) extends TimeService {
  val ClockRealtime = 0
  val ClockTAI      = 11

  override def UTCTime(): CswInstant = time(TimeScales.UTCScale)

  override def TAITime(): CswInstant = time(TimeScales.TAIScale)

  private val library: TimeLibrary = new TimeLibrary()

  override def TAIOffset(): Int = {
    val timeVal = new NTPTimeVal()
    library.ntp_gettimex(timeVal)
    timeVal.tai
  }

  private def time(scale: TimeScale): CswInstant = {
    val timeSpec = new TimeSpec()

    val clock = scale match {
      case TimeScales.UTCScale => ClockRealtime
      case TimeScales.TAIScale => ClockTAI
    }

    library.clock_gettime(clock, timeSpec)

    val instant = Instant.ofEpochSecond(timeSpec.seconds, timeSpec.nanoseconds)
    CswInstant(instant, scale)
  }

}
