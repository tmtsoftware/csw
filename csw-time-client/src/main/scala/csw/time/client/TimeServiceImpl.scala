package csw.time.client

import java.time.{Clock, Instant}

import com.sun.jna.Native
import csw.time.api.TimeService

class TimeServiceImpl(clock: Clock) extends TimeService {
  val ClockRealtime = 0

  override def UTCTime(): Instant = {
    val timeLibrary = Native.load("c", classOf[TimeLibrary])
    val timeSpec    = new TimeSpec()

    timeLibrary.clock_gettime(ClockRealtime, timeSpec)

    Instant.ofEpochSecond(timeSpec.seconds, timeSpec.nanoseconds)
  }

}
