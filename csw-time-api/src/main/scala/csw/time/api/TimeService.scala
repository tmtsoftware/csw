package csw.time.api
import java.time.Duration

import akka.actor.ActorRef
import csw.time.api.models.Cancellable

trait TimeService {
  def scheduleOnce(startTime: TAITime)(f: ⇒ Unit): Cancellable
  def scheduleOnce(startTime: TAITime, runnable: Runnable): Cancellable
  def scheduleOnce(startTime: TAITime, receiver: ActorRef, message: Any): Cancellable

  def schedulePeriodically(duration: Duration)(f: ⇒ Unit): Cancellable
  def schedulePeriodically(duration: Duration, runnable: Runnable): Cancellable
  def schedulePeriodically(duration: Duration, receiver: ActorRef, message: Any): Cancellable

  def schedulePeriodically(startTime: TAITime, duration: Duration)(f: ⇒ Unit): Cancellable
  def schedulePeriodically(startTime: TAITime, duration: Duration, runnable: Runnable): Cancellable
  def schedulePeriodically(startTime: TAITime, duration: Duration, receiver: ActorRef, message: Any): Cancellable
}
