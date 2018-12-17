package csw.time.client.internal

import java.time.Duration
import java.util.concurrent.TimeUnit.NANOSECONDS

import akka.actor.ActorSystem
import csw.time.api.models.Cancellable
import csw.time.api.{TAITime, TimeService}
import csw.time.client.internal.extensions.RichCancellableExt.RichCancellable

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class TimeServiceImpl(implicit actorSystem: ActorSystem) extends TimeService {
  import actorSystem.dispatcher

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

  private def delayFrom(time: TAITime): FiniteDuration = {
    val now      = TAITime.now().value
    val duration = Duration.between(now, time.value)
    FiniteDuration(duration.toNanos, NANOSECONDS)
  }
}
