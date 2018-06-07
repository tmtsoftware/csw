package csw.services.event.scaladsl

import akka.Done
import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import csw.messages.events.Event
import csw.services.event.exceptions.PublishFailure

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

trait EventPublisher {

  // publish a single event
  def publish(event: Event): Future[Done]

  // publish from a stream of events
  def publish[Mat](source: Source[Event, Mat]): Mat

  // publish from a stream of events, and execute `onError` callback for each publish failed event
  def publish[Mat](source: Source[Event, Mat], onError: PublishFailure ⇒ Unit): Mat

  // publish from a `eventGenerator` function, which will be executed at `every` frequency. `Cancellable` can be used to cancel
  // the execution of `eventGenerator` function.
  def publish(eventGenerator: => Event, every: FiniteDuration): Cancellable

  // publish from a `eventGenerator` function, which will be executed at `every` frequency. Also, provide `onError` callback
  // for each publish failed event. Note that any exception thrown from `eventGenerator` or `onError` callback is expected
  // to be handled by component developers.
  def publish(eventGenerator: ⇒ Event, every: FiniteDuration, onError: PublishFailure ⇒ Unit): Cancellable

  // shuts down the connection for this publisher, using any api of publisher after shutting should give exceptions.
  // This method will be called while component is getting shutdown gracefully.
  def shutdown(): Future[Done]
}
