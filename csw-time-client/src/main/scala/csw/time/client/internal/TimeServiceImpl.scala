package csw.time.client.internal

import java.time.Duration
import java.util.concurrent.TimeUnit.NANOSECONDS

import akka.actor.ActorSystem
import csw.time.api.models.Cancellable
import csw.time.api.models.CswInstant.{TaiInstant, UtcInstant}
import csw.time.api.scaladsl.TimeService
import csw.time.client.internal.extensions.RichCancellableExt.RichCancellable

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class TimeServiceImpl(clock: TMTClock)(implicit actorSystem: ActorSystem) extends TimeService {

  override def utcTime(): UtcInstant = clock.utcInstant

  override def taiTime(): TaiInstant = clock.taiInstant

  override def taiOffset(): Int = clock.offset

  override def scheduleOnce(startTime: TaiInstant)(task: Runnable): Cancellable = {
    val delay                 = delayFrom(startTime)
    val underlyingCancellable = actorSystem.scheduler.scheduleOnce(delay)(task.run())(actorSystem.dispatcher)
    underlyingCancellable.toTsCancellable
  }

  override def schedulePeriodically(interval: Duration)(task: Runnable): Cancellable = {
    val i                     = FiniteDuration(interval.toNanos, NANOSECONDS)
    val underlyingCancellable = actorSystem.scheduler.schedule(0.millis, i)(task.run())(actorSystem.dispatcher)
    underlyingCancellable.toTsCancellable
  }

  override def schedulePeriodically(startTime: TaiInstant, interval: Duration)(task: Runnable): Cancellable = {
    val delay                 = delayFrom(startTime)
    val i                     = FiniteDuration(interval.toNanos, NANOSECONDS)
    val underlyingCancellable = actorSystem.scheduler.schedule(delay, i)(task.run())(actorSystem.dispatcher)
    underlyingCancellable.toTsCancellable
  }

  override def toUtc(taiInstant: TaiInstant): UtcInstant = UtcInstant(taiInstant.value.minusSeconds(clock.offset))

  override def toTai(utcInstant: UtcInstant): TaiInstant = TaiInstant(utcInstant.value.plusSeconds(clock.offset))

  private[time] def setTaiOffset(offset: Int): Unit = clock.setOffset(offset)

  private def delayFrom(time: TaiInstant): FiniteDuration = {
    val now      = taiTime().value
    val duration = Duration.between(now, time.value)
    FiniteDuration(duration.toNanos, NANOSECONDS)
  }
}
