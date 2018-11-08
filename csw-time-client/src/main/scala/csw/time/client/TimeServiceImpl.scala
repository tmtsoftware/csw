package csw.time.client

import java.time.{Clock, Instant}

import csw.time.api.TimeService

class TimeServiceImpl(clock: Clock) extends TimeService {
  val ClockRealtime = 0
  val ClockTAI      = 11

  override def UTCTime(): Instant = time(ClockRealtime)

  override def TAITime(): Instant = time(ClockTAI)

  private def time(clock: Int): Instant = {
    val timeSpec = new TimeSpec()

    TimeLibrary2.clock_gettime(clock, timeSpec)

    Instant.ofEpochSecond(timeSpec.seconds, timeSpec.nanoseconds)
  }

}
