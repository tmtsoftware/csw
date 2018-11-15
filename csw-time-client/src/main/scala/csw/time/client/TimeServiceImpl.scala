package csw.time.client

import java.time.{Clock, Instant}

import csw.time.api.TimeService

class TimeServiceImpl(clock: Clock) extends TimeService {
  val ClockRealtime = 0
  val ClockTAI      = 11

  override def UTCTime(): Instant = time(ClockRealtime)

  override def TAITime(): Instant = time(ClockTAI)

  private val library: TimeLibrary = new TimeLibrary()

  override def TAIOffset(): Int = {
    val timeVal = new NTPTimeVal()
    library.ntp_gettimex(timeVal)
    timeVal.tai
  }

  private def time(clock: Int): Instant = {
    val timeSpec = new TimeSpec()
    library.clock_gettime(clock, timeSpec)

    Instant.ofEpochSecond(timeSpec.seconds, timeSpec.nanoseconds)
  }

}
