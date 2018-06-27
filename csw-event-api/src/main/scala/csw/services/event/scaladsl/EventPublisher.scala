package csw.services.event.scaladsl

import akka.Done
import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import csw.messages.events.Event
import csw.services.event.exceptions.PublishFailure

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
 * An EventPublisher interface to publish events. The published events are published on a key determined by [[csw.messages.events.EventKey]]
 * in the [[csw.messages.events.Event]] model. This key can be used by the subscribers using [[csw.services.event.scaladsl.EventSubscriber]]
 * interface to subscribe to the events.
 */
trait EventPublisher {

  /**
   * publish a single [[csw.messages.events.Event]]
   *
   * @param event an event to be published
   * @return a future which completes when the event is published
   */
  def publish(event: Event): Future[Done]

  /**
   * publish from a stream of [[csw.messages.events.Event]]
   *
   * @param source a [[akka.stream.scaladsl.Source]] of events to be published
   * @tparam Mat represents the type of materialized value as defined in the source to be obtained on running the stream
   * @return the materialized value obtained on running the stream
   */
  def publish[Mat](source: Source[Event, Mat]): Mat

  /**
   * publish from a stream of [[csw.messages.events.Event]], and execute `onError` callback for each event for which publishing failed
   * @param source a [[akka.stream.scaladsl.Source]] of events to be published
   * @param onError a callback to execute for each event for which publishing failed
   * @tparam Mat represents the type of materialized value as defined in the source to be obtained on running the stream
   * @return the materialized value obtained on running the stream
   */
  def publish[Mat](source: Source[Event, Mat], onError: PublishFailure ⇒ Unit): Mat

  /**
   * publish [[csw.messages.events.Event]] from a `eventGenerator` function, which will be executed at `every` frequency. `Cancellable` can be used to cancel
   * the execution of `eventGenerator` function.
   *
   * @param eventGenerator a function which can generate an event to be published at `every` frequency
   * @param every frequency with which the events are to be published
   * @return a handle to cancel the event generation through `eventGenerator`
   */
  def publish(eventGenerator: => Event, every: FiniteDuration): Cancellable

  /**
   * publish [[csw.messages.events.Event]] from a `eventGenerator` function, which will be executed at `every` frequency. Also, provide `onError` callback
   * for each event for which publishing failed.
   *
   * @note any exception thrown from `eventGenerator` or `onError` callback is expected
   * to be handled by component developers.
   * @param eventGenerator a function which can generate an event to be published at `every` frequency
   * @param every frequency with which the events are to be published
   * @param onError a callback to execute for each event for which publishing failed
   * @return @return a handle to cancel the event generation through `eventGenerator`
   */
  def publish(eventGenerator: ⇒ Event, every: FiniteDuration, onError: PublishFailure ⇒ Unit): Cancellable

  /**
   * shuts down the connection for this publisher. Using any api of publisher after shutdown should give exceptions.
   * This method should be called while the component is shutdown gracefully.
   *
   * @return a future which completes when the event is published
   */
  private[event] def shutdown(): Future[Done]
}
