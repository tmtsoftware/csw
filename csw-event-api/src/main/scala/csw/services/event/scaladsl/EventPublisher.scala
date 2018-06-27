package csw.services.event.scaladsl

import akka.Done
import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import csw.messages.events.Event
import csw.services.event.exceptions.PublishFailure

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
 * An EventPublisher interface to publish events.
 */
trait EventPublisher {

  /**
   * publish a single event
   * @param event an event to be published
   * @return a future which completes when the event is published
   */
  def publish(event: Event): Future[Done]

  /**
   * publish from a stream of events
   * @param source a [[akka.stream.scaladsl.Source]] of events to be published
   * @tparam Mat represents the type of materialized value as defined in the source to be obtained on running the stream
   * @return the materialized value obtained on running the stream
   */
  def publish[Mat](source: Source[Event, Mat]): Mat

  /**
   * publish from a stream of events, and execute `onError` callback for each event for which publishing failed
   * @param source a [[akka.stream.scaladsl.Source]] of events to be published
   * @param onError a callback to execute for each event for which publishing failed
   * @tparam Mat represents the type of materialized value as defined in the source to be obtained on running the stream
   * @return the materialized value obtained on running the stream
   */
  def publish[Mat](source: Source[Event, Mat], onError: PublishFailure ⇒ Unit): Mat

  /**
   * publish from a `eventGenerator` function, which will be executed at `every` frequency. `Cancellable` can be used to cancel
   * the execution of `eventGenerator` function.
   * @param eventGenerator a function which can generate an event to be published at `every` frequency
   * @param every frequency with which the events are to be published
   * @return a handle to cancel the event generation through `eventGenerator`
   */
  def publish(eventGenerator: => Event, every: FiniteDuration): Cancellable

  /**
   * publish from a `eventGenerator` function, which will be executed at `every` frequency. Also, provide `onError` callback
   * for each event for which publishing failed.
   * @note any exception thrown from `eventGenerator` or `onError` callback is expected
   * to be handled by component developers.
   * @param eventGenerator a function which can generate an event to be published at `every` frequency
   * @param every frequency with which the events are to be published
   * @param onError a callback to execute for each event for which publishing failed
   * @return @return a handle to cancel the event generation through `eventGenerator`
   */
  def publish(eventGenerator: ⇒ Event, every: FiniteDuration, onError: PublishFailure ⇒ Unit): Cancellable

  /**
   * shuts down the connection for this publisher, using any api of publisher after shutting should give exceptions.
   * This method will be called while component is getting shutdown gracefully.
   * @return a future which completes when the event is published
   */
  private[event] def shutdown(): Future[Done]
}
