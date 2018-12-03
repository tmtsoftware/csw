package csw.time.client.internal

import java.time.{Duration, Instant}
import java.util.concurrent.TimeUnit.NANOSECONDS

import akka.actor.ActorSystem
import com.sun.jna.NativeLong
import csw.time.api.models.Cancellable
import csw.time.api.models.CswInstant.{TaiInstant, UtcInstant}
import csw.time.api.scaladsl.TimeService
import csw.time.client.internal.extensions.RichCancellableExt.RichCancellable
import csw.time.client.internal.native_models.ClockId.{ClockRealtime, ClockTAI}
import csw.time.client.internal.native_models._

import scala.concurrent.duration.FiniteDuration

class TimeServiceImpl(implicit actorSystem: ActorSystem) extends TimeService {
  override def utcTime(): UtcInstant = UtcInstant(instantFor(ClockRealtime))

  override def taiTime(): TaiInstant = TaiInstant(instantFor(ClockTAI))

  override def taiOffset(): Int = {
    val timeVal = new NTPTimeVal()
    TimeLibrary.ntp_gettimex(timeVal)
    timeVal.tai
  }

  override def scheduleOnce(startTime: TaiInstant)(task: => Unit): Cancellable = {
    val delay                 = delayFrom(startTime)
    val underlyingCancellable = actorSystem.scheduler.scheduleOnce(delay)(task)(actorSystem.dispatcher)
    underlyingCancellable.toTsCancellable
  }

  private def instantFor(clockId: Int): Instant = {
    val timeSpec = new TimeSpec()
    TimeLibrary.clock_gettime(clockId, timeSpec)

    Instant.ofEpochSecond(timeSpec.seconds.longValue(), timeSpec.nanoseconds.longValue())
  }

  private def delayFrom(time: TaiInstant): FiniteDuration = {
    val now      = taiTime().value
    val duration = Duration.between(now, time.value)
    FiniteDuration(duration.toNanos, NANOSECONDS)
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

}
