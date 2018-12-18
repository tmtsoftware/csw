package csw.time.api
import java.time.Duration

import akka.actor.ActorRef
import csw.time.api.models.Cancellable

trait TimeServiceScheduler {
  def scheduleOnce(startTime: TMTTime)(f: ⇒ Unit): Cancellable
  def scheduleOnce(startTime: TMTTime, runnable: Runnable): Cancellable
  def scheduleOnce(startTime: TMTTime, receiver: ActorRef, message: Any): Cancellable

  def schedulePeriodically(duration: Duration)(f: ⇒ Unit): Cancellable
  def schedulePeriodically(duration: Duration, runnable: Runnable): Cancellable
  def schedulePeriodically(duration: Duration, receiver: ActorRef, message: Any): Cancellable

  def schedulePeriodically(startTime: TMTTime, duration: Duration)(f: ⇒ Unit): Cancellable
  def schedulePeriodically(startTime: TMTTime, duration: Duration, runnable: Runnable): Cancellable
  def schedulePeriodically(startTime: TMTTime, duration: Duration, receiver: ActorRef, message: Any): Cancellable
}
