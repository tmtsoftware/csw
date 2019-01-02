package csw.clock.models

import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

import com.sun.jna.NativeLong
import csw.clock.models.ClockId.{ClockRealtime, ClockTAI}
import csw.clock.natives.TimeLibrary
import csw.clock.natives.models.{NTPTimeVal, TimeSpec, Timex}

import scala.util.Try
import scala.util.control.NonFatal

sealed trait TMTClock {
  def utcInstant: Instant
  def taiInstant: Instant
  def offset: Int
  def setTaiOffset(offset: Int): Unit
}
object TMTClock {
  val clock: TMTClock = OSType.value match {
    case OSType.Linux => new LinuxClock()
    case OSType.Other => new NonLinuxClock()
  }
}

class LinuxClock extends TMTClock {

  override def utcInstant: Instant = now(ClockRealtime)
  override def taiInstant: Instant = now(ClockTAI)

  private def now(clockId: Int): Instant = {
    val timeSpec = new TimeSpec()
    TimeLibrary.clock_gettime(clockId, timeSpec)
    Instant.ofEpochSecond(timeSpec.seconds.longValue(), timeSpec.nanoseconds.longValue())
  }

  override def offset: Int = {
    val timeVal = new NTPTimeVal()
    TimeLibrary.ntp_gettimex(timeVal)
    timeVal.tai
  }

  // todo: without sudo or somehow handle it internally?
  // sets the tai offset on kernel (needed when ptp is not setup)
  override def setTaiOffset(offset: Int): Unit = {
    val timex = new Timex()
    timex.modes = 128
    timex.constant = new NativeLong(offset)
    Try(TimeLibrary.ntp_adjtime(timex)).recover {
      case NonFatal(e) =>
        throw new RuntimeException("Failed to set offset, make sure you have sudo access to perform this action.", e.getCause)
    }
  }
}

class NonLinuxClock extends TMTClock {
  private val internal_offset: AtomicInteger = new AtomicInteger(0)

  override def offset: Int         = internal_offset.get()
  override def utcInstant: Instant = Instant.now()
  override def taiInstant: Instant = Instant.now().plusSeconds(offset)
  // This api is only for testing purpose and might not set offset value in one attempt in concurrent environment
  override def setTaiOffset(_offset: Int): Unit = internal_offset.compareAndSet(offset, _offset)
}
