package csw.services.event.javadsl

import java.util.concurrent.CompletableFuture
import java.util.function.{Consumer, Supplier}

import akka.Done
import akka.actor.Cancellable
import akka.stream.javadsl.Source
import csw.messages.events.Event
import csw.services.event.exceptions.PublishFailure

import scala.concurrent.duration.FiniteDuration

/**
 * An EventPublisher interface to publish events. The published events are published on a key determined by [[csw.messages.events.EventKey]]
 * in the [[csw.messages.events.Event]] model. This key can be used by the subscribers using [[csw.services.event.javadsl.IEventSubscriber]]
 * interface to subscribe to the events.
 */
trait IEventPublisher {

  /**
   * Publish a single [[csw.messages.events.Event]]
   *
   * @param event an event to be published
   * @return a completable future which completes when the event is published
   */
  def publish(event: Event): CompletableFuture[Done]

  /**
   * Publish from a stream of [[csw.messages.events.Event]]
   *
   * @param source a [[akka.stream.javadsl.Source]] of events to be published
   * @tparam Mat represents the type of materialized value as defined in the source to be obtained on running the stream
   * @return the materialized value obtained on running the stream
   */
  def publish[Mat](source: Source[Event, Mat]): Mat

  /**
   * Publish from a stream of [[csw.messages.events.Event]], and perform an operation defined using `onError` consumer for each event
   * for which publishing failed
   *
   * @param source a [[akka.stream.javadsl.Source]] of events to be published
   * @param onError a consumer which defines an operation for each event for which publishing failed
   * @tparam Mat represents the type of materialized value as defined in the source to be obtained on running the stream
   * @return the materialized value obtained on running the stream
   */
  def publish[Mat](source: Source[Event, Mat], onError: Consumer[PublishFailure]): Mat

  /**
   * Publish [[csw.messages.events.Event]] from an `eventGenerator` supplier, which will be executed at `every` frequency. `Cancellable` can be used to cancel
   * the execution of `eventGenerator` function.
   *
   * @param eventGenerator a supplier which can generate an event to be published at `every` frequency
   * @param every frequency with which the events are to be published
   * @return a handle to cancel the event generation through `eventGenerator`
   */
  def publish(eventGenerator: Supplier[Event], every: FiniteDuration): Cancellable

  /**
   * Publish [[csw.messages.events.Event]] from an `eventGenerator` supplier, which will be executed at `every` frequency. Also, provide `onError` consumer to
   * perform an operation for each event for which publishing failed.
   *
   * @note any exception thrown from `eventGenerator` or `onError` callback is expected
   * to be handled by component developers.
   * @param eventGenerator a function which can generate an event to be published at `every` frequency
   * @param every frequency with which the events are to be published
   * @param onError a consumer which defines an operation for each event for which publishing failed
   * @return a handle to cancel the event generation through `eventGenerator`
   */
  def publish(eventGenerator: Supplier[Event], every: FiniteDuration, onError: Consumer[PublishFailure]): Cancellable

  /**
   * Shuts down the connection for this publisher. Using any api of publisher after shutdown should give exceptions.
   * This method should be called while the component is shutdown gracefully.
   *
   * @return a future which completes when the underlying connection is shut down
   */
  private[event] def shutdown(): CompletableFuture[Done]
}
