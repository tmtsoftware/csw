package csw.time.client.internal

import java.time.{Duration, Instant}
import java.util.concurrent.TimeUnit.NANOSECONDS

import akka.actor.ActorSystem
import com.sun.jna.NativeLong
import csw.time.api.models.Cancellable
import csw.time.api.models.CswInstant.{TaiInstant, UtcInstant}
import csw.time.api.scaladsl.TimeService
import csw.time.client.internal.TimeLibraryUtil.Linux
import csw.time.client.internal.extensions.RichCancellableExt.RichCancellable
import csw.time.client.internal.native_models.{NTPTimeVal, TimeSpec, Timex}
import csw.time.client.internal.native_models.ClockId.{ClockRealtime, ClockTAI}
import csw.time.client.internal.native_models._

import scala.concurrent.duration.FiniteDuration

class TimeServiceImpl() extends TimeService {
  val ClockRealtime       = 0
  val ClockTAI            = 11
  private val actorSystem = ActorSystem("TimeService")
  private val osType      = TimeLibraryUtil.osType

class TimeServiceImpl(implicit actorSystem: ActorSystem) extends TimeService {
  override def utcTime(): UtcInstant = UtcInstant(instantFor(ClockRealtime))

  override def taiTime(): TaiInstant = TaiInstant(instantFor(ClockTAI))

  override def taiOffset(): Int = {
    val timeVal = new NTPTimeVal()
    osType match {
      case Linux =>
        TimeLibrary.ntp_gettimex(timeVal)
      case _ =>
        TimeLibraryOther.ntp_gettimex(timeVal)
    }
    timeVal.tai
  }

  override def scheduleOnce(startTime: TaiInstant)(task: => Unit): Cancellable = {
    val delay                 = delayFrom(startTime)
    val underlyingCancellable = actorSystem.scheduler.scheduleOnce(delay)(task)(actorSystem.dispatcher)
    underlyingCancellable.toTsCancellable
  }

  private def instantFor(clockId: Int): Instant = {
    val timeSpec = new TimeSpec()
    osType match {
      case Linux =>
        TimeLibrary.clock_gettime(clockId, timeSpec)
      case _ =>
        TimeLibraryOther.clock_gettime(clockId, timeSpec)
    }
    Instant.ofEpochSecond(timeSpec.seconds.longValue(), timeSpec.nanoseconds.longValue())
  }

  private def delayFrom(time: TaiInstant): FiniteDuration = {
    val now      = taiTime().value
    val duration = Duration.between(now, time.value)
    FiniteDuration(duration.toNanos, NANOSECONDS)
  }
}

/**
 * A utility class to support the time library adjustment calls and to check the OS type
 */
object TimeLibraryUtil {

  val osType = getOperatingSystemType

  def ntp_adjtime(timex: Timex): Int = {
    osType match {
      case Linux =>
        TimeLibrary.ntp_adjtime(timex)
      case _ =>
        TimeLibraryOther.ntp_adjtime(timex)
    }
  }

  // todo: without sudo or somehow handle it internally?
  // sets the tai offset on kernel (needed when ptp is not setup)
  private[time] def setTaiOffset(offset: Int): Unit = {
    val timex = new Timex()

    timex.modes = 128
    timex.constant = new NativeLong(offset)
    TimeLibrary.ntp_adjtime(timex)
    println(s"Status of Tai offset command=" + timex.status)
    println(s"Tai offset set to [${taiOffset()}]")
  }

  def ntp_gettimex(timex: NTPTimeVal): Int = {
    osType match {
      case Linux =>
        TimeLibrary.ntp_gettimex(timex)
      case _ =>
        TimeLibraryOther.ntp_gettimex(timex)
    }
  }

  sealed trait OSType
  case object MacOS extends OSType
  case object Linux extends OSType
  case object Other extends OSType

  /**
   * detect the operating system from the os.name System property
   *
   * @returns - the operating system detected
   */
  def getOperatingSystemType: OSType = {
    import java.util.Locale

    val OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH)

    if ((OS.indexOf("mac") >= 0) || (OS.indexOf("darwin") >= 0)) MacOS
    else if (OS.indexOf("nux") >= 0) Linux
    else Other
  }
}
