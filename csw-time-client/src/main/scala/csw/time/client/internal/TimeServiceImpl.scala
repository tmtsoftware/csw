package csw.time.client.internal

import java.time.Duration
import java.util.concurrent.TimeUnit.NANOSECONDS

import akka.actor.{ActorRef, ActorSystem, Scheduler}
import csw.time.api.models.Cancellable
import csw.time.api.{TAITime, TimeService}
import csw.time.client.internal.extensions.RichCancellableExt.RichCancellable

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class TimeServiceImpl(implicit actorSystem: ActorSystem) extends TimeService {
  import actorSystem.dispatcher

  private val scheduler: Scheduler = actorSystem.scheduler

  // ========== scheduleOnce ==========
  override def scheduleOnce(startTime: TAITime)(f: ⇒ Unit): Cancellable =
    scheduler.scheduleOnce(delayFrom(startTime))(f).toTsCancellable

  override def scheduleOnce(startTime: TAITime, runnable: Runnable): Cancellable =
    scheduler.scheduleOnce(delayFrom(startTime), runnable).toTsCancellable

  override def scheduleOnce(startTime: TAITime, receiver: ActorRef, message: Any): Cancellable =
    scheduler.scheduleOnce(delayFrom(startTime), receiver, message).toTsCancellable

  // ========== schedulePeriodically ==========
  override def schedulePeriodically(interval: Duration, runnable: Runnable): Cancellable =
    scheduler.schedule(0.millis, FiniteDuration(interval.toNanos, NANOSECONDS), runnable).toTsCancellable

  override def schedulePeriodically(interval: Duration)(f: ⇒ Unit): Cancellable =
    scheduler.schedule(0.millis, FiniteDuration(interval.toNanos, NANOSECONDS))(f).toTsCancellable

  override def schedulePeriodically(interval: Duration, receiver: ActorRef, message: Any): Cancellable =
    scheduler.schedule(0.millis, FiniteDuration(interval.toNanos, NANOSECONDS), receiver, message).toTsCancellable

  // ========== schedulePeriodically with start time ==========
  override def schedulePeriodically(startTime: TAITime, interval: Duration, runnable: Runnable): Cancellable =
    scheduler.schedule(delayFrom(startTime), FiniteDuration(interval.toNanos, NANOSECONDS), runnable).toTsCancellable

  override def schedulePeriodically(startTime: TAITime, interval: Duration)(f: ⇒ Unit): Cancellable =
    scheduler.schedule(delayFrom(startTime), FiniteDuration(interval.toNanos, NANOSECONDS))(f).toTsCancellable

  override def schedulePeriodically(startTime: TAITime, interval: Duration, receiver: ActorRef, message: Any): Cancellable =
    scheduler.schedule(delayFrom(startTime), FiniteDuration(interval.toNanos, NANOSECONDS), receiver, message).toTsCancellable

  private def delayFrom(time: TAITime): FiniteDuration = {
    val now      = TAITime.now().value
    val duration = Duration.between(now, time.value)
    FiniteDuration(duration.toNanos, NANOSECONDS)
  }
}
