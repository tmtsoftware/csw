package csw.time.clock.natives.models

import java.time.Instant

import csw.time.clock.natives.TimeLibrary
import csw.time.clock.natives.models.ClockId.{ClockRealtime, ClockTAI}

private[time] sealed trait TMTClock {
  def utcInstant: Instant
  def taiInstant: Instant
  def offset: Int
}
private[time] object TMTClock {
  val clock: TMTClock = OSType.value match {
    case OSType.Linux => new LinuxClock()
    case OSType.Other => new NonLinuxClock()
  }
}

private[time] class LinuxClock extends TMTClock {

  override def utcInstant: Instant = now(ClockRealtime)
  override def taiInstant: Instant = now(ClockTAI)

  //#native-calls
  private def now(clockId: Int): Instant = {
    val timeSpec = new TimeSpec()
    TimeLibrary.clock_gettime(clockId, timeSpec)
    Instant.ofEpochSecond(timeSpec.seconds.longValue(), timeSpec.nanoseconds.longValue())
  }

  override def offset: Int = {
    val timeVal = new NTPTimeVal()
    TimeLibrary.ntp_gettimex(timeVal)
    if (timeVal.tai == 0)
      println("=================================================================================================")
    println("WARNING: Value of TAI OFFSET is 0. To set the TAI OFFSET on your machine,")
    println(
      "please follow instructions in TimeService Documentation [https://tmtsoftware.github.io/csw/services/time.html#dependencies]"
    )
    println("=================================================================================================")
    timeVal.tai
  }
  //#native-calls
}

private[time] class NonLinuxClock extends TMTClock {
  override def offset: Int         = TimeConstants.taiOffset
  override def utcInstant: Instant = Instant.now()
  override def taiInstant: Instant = Instant.now().plusSeconds(offset)
}
