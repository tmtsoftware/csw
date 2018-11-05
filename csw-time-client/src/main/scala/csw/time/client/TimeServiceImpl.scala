package csw.time.client

import java.time.{Clock, Instant}

import com.sun.jna.Native
import csw.time.api.TimeService

class TimeServiceImpl(clock: Clock) extends TimeService {
  val ClockRealtime = 0
  val ClockTAI      = 11

  override def UTCTime(): Instant = time(ClockRealtime)

  override def TAITime(): Instant = time(ClockTAI)

  private def time(clock: Int): Instant = {
    val timeLibrary = Native.load("c", classOf[TimeLibrary])
    val timeSpec    = new TimeSpec()

    timeLibrary.clock_gettime(clock, timeSpec)

    Instant.ofEpochSecond(timeSpec.seconds, timeSpec.nanoseconds)
  }

}
