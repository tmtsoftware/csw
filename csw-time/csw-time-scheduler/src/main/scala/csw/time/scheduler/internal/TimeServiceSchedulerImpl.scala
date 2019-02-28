package csw.time.scheduler.internal

import java.time.Duration
import java.util.concurrent.TimeUnit.NANOSECONDS

import akka.actor.{ActorRef, ActorSystem, Scheduler}
import csw.time.core.models.TMTTime
import csw.time.scheduler.api.{Cancellable, TimeServiceScheduler}
import csw.time.scheduler.internal.extensions.RichCancellableExt.RichCancellable

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class TimeServiceSchedulerImpl(implicit actorSystem: ActorSystem) extends TimeServiceScheduler {
  import actorSystem.dispatcher

  private val scheduler: Scheduler = actorSystem.scheduler

  // ========== scheduleOnce ==========
  override def scheduleOnce(startTime: TMTTime)(task: ⇒ Unit): Cancellable =
    scheduler.scheduleOnce(startTime.durationFromNow)(task).toTsCancellable

  override def scheduleOnce(startTime: TMTTime, task: Runnable): Cancellable =
    scheduler.scheduleOnce(startTime.durationFromNow, task).toTsCancellable

  override def scheduleOnce(startTime: TMTTime, receiver: ActorRef, message: Any): Cancellable =
    scheduler.scheduleOnce(startTime.durationFromNow, receiver, message).toTsCancellable

  // ========== schedulePeriodically ==========
  override def schedulePeriodically(interval: Duration, task: Runnable): Cancellable =
    scheduler.schedule(0.millis, FiniteDuration(interval.toNanos, NANOSECONDS), task).toTsCancellable

  override def schedulePeriodically(interval: Duration)(task: ⇒ Unit): Cancellable =
    scheduler.schedule(0.millis, FiniteDuration(interval.toNanos, NANOSECONDS))(task).toTsCancellable

  override def schedulePeriodically(interval: Duration, receiver: ActorRef, message: Any): Cancellable =
    scheduler.schedule(0.millis, FiniteDuration(interval.toNanos, NANOSECONDS), receiver, message).toTsCancellable

  // ========== schedulePeriodically with start time ==========
  override def schedulePeriodically(startTime: TMTTime, interval: Duration, task: Runnable): Cancellable =
    scheduler.schedule(startTime.durationFromNow, FiniteDuration(interval.toNanos, NANOSECONDS), task).toTsCancellable

  override def schedulePeriodically(startTime: TMTTime, interval: Duration)(task: ⇒ Unit): Cancellable =
    scheduler.schedule(startTime.durationFromNow, FiniteDuration(interval.toNanos, NANOSECONDS))(task).toTsCancellable

  override def schedulePeriodically(startTime: TMTTime, interval: Duration, receiver: ActorRef, message: Any): Cancellable =
    scheduler
      .schedule(startTime.durationFromNow, FiniteDuration(interval.toNanos, NANOSECONDS), receiver, message)
      .toTsCancellable

}
