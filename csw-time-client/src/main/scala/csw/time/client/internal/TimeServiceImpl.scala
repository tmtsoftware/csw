package csw.time.client.internal

import java.time.Duration
import java.util.concurrent.TimeUnit.NANOSECONDS

import akka.actor.ActorSystem
import csw.time.api.models.Cancellable
import csw.time.api.models.TMTTime.{TAITime, UTCTime}
import csw.time.api.scaladsl.TimeService
import csw.time.client.internal.extensions.RichCancellableExt.RichCancellable

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class TimeServiceImpl(clock: TMTClock)(implicit actorSystem: ActorSystem) extends TimeService {
  import actorSystem.dispatcher

  override def utcTime(): UTCTime = clock.utcTime()

  override def taiTime(): TAITime = clock.taiTime()

  override def scheduleOnce(startTime: TAITime)(task: Runnable): Cancellable =
    actorSystem.scheduler
      .scheduleOnce(delayFrom(startTime))(task.run())
      .toTsCancellable

  override def schedulePeriodically(interval: Duration)(task: Runnable): Cancellable =
    actorSystem.scheduler
      .schedule(0.millis, FiniteDuration(interval.toNanos, NANOSECONDS))(task.run())
      .toTsCancellable

  override def schedulePeriodically(startTime: TAITime, interval: Duration)(task: Runnable): Cancellable =
    actorSystem.scheduler
      .schedule(delayFrom(startTime), FiniteDuration(interval.toNanos, NANOSECONDS))(task.run())
      .toTsCancellable

  override def toUTC(taiInstant: TAITime): UTCTime = UTCTime(taiInstant.value.minusSeconds(clock.offset))

  override def toTAI(utcInstant: UTCTime): TAITime = TAITime(utcInstant.value.plusSeconds(clock.offset))

  private[time] def setTaiOffset(offset: Int): Unit = clock.setOffset(offset)

  private def delayFrom(time: TAITime): FiniteDuration = {
    val now      = taiTime().at(time.value.getZone).value // use same zone as provided in time for duration calculation
    val duration = Duration.between(now, time.value)
    FiniteDuration(duration.toNanos, NANOSECONDS)
  }
}
