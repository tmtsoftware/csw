package csw.services.event.javadsl

import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.function.{Consumer, Supplier}

import akka.Done
import akka.actor.Cancellable
import akka.stream.javadsl.Source
import csw.messages.events.Event
import csw.services.event.exceptions.PublishFailure

/**
 * An EventPublisher interface to publish events. The published events are published on a key determined by [[csw.messages.events.EventKey]]
 * in the [[csw.messages.events.Event]] model. This key can be used by the subscribers using [[csw.services.event.javadsl.IEventSubscriber]]
 * interface to subscribe to the events.
 */
trait IEventPublisher {

  /**
   * Publish a single [[csw.messages.events.Event]]
   *
   * At the time of invocation, in case the underlying server is not available, [[csw.services.event.exceptions.EventServerNotAvailable]] exception is thrown,
   * in all other cases [[csw.services.event.exceptions.PublishFailure]] exception is thrown which wraps the underlying exception and
   * also provides the handle to the event which was failed to be published
   *
   * @param event an event to be published
   * @return a completable future which completes when the event is published
   */
  def publish(event: Event): CompletableFuture[Done]

  /**
   * Publish from a stream of [[csw.messages.events.Event]]
   *
   * At the time of invocation, in case the underlying server is not available, [[csw.services.event.exceptions.EventServerNotAvailable]] exception is thrown and the stream is
   * stopped after logging appropriately. In all other cases of exception, the stream receives a [[csw.services.event.exceptions.PublishFailure]] exception
   * which wraps the underlying exception. The stream resumes to publish remaining elements in case of this exception.
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
   * At the time of invocation, in case the underlying server is not available, [[csw.services.event.exceptions.EventServerNotAvailable]] exception is thrown and the stream is
   * stopped after logging appropriately. In all other cases of exception, the stream receives a [[csw.services.event.exceptions.PublishFailure]] exception
   * which wraps the underlying exception and also provides the handle to the event which was failed to be published.
   * The provided callback is executed on the failed element and the stream resumes to publish remaining elements.
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
   * At the time of invocation, in case the underlying server is not available, [[csw.services.event.exceptions.EventServerNotAvailable]] exception is thrown and the stream is
   * stopped after logging appropriately. In all other cases of exception, the stream receives a [[csw.services.event.exceptions.PublishFailure]] exception
   * which wraps the underlying exception. The generator resumes to publish remaining elements in case of this exception.
   *
   * @param eventGenerator a supplier which can generate an event to be published at `every` frequency
   * @param every frequency with which the events are to be published
   * @return a handle to cancel the event generation through `eventGenerator`
   */
  def publish(eventGenerator: Supplier[Event], every: Duration): Cancellable

  /**
   * Publish [[csw.messages.events.Event]] from an `eventGenerator` supplier, which will be executed at `every` frequency. Also, provide `onError` consumer to
   * perform an operation for each event for which publishing failed.
   *
   * At the time of invocation, in case the underlying server is not available, [[csw.services.event.exceptions.EventServerNotAvailable]] exception is thrown and the stream is
   * stopped after logging appropriately. In all other cases of exception, the stream receives a [[csw.services.event.exceptions.PublishFailure]] exception
   * which wraps the underlying exception and also provides the handle to the event which was failed to be published.
   * The provided callback is executed on the failed element and the generator resumes to publish remaining elements.
   *
   * @note any exception thrown from `eventGenerator` or `onError` callback is expected
   * to be handled by component developers.
   * @param eventGenerator a function which can generate an event to be published at `every` frequency
   * @param every frequency with which the events are to be published
   * @param onError a consumer which defines an operation for each event for which publishing failed
   * @return a handle to cancel the event generation through `eventGenerator`
   */
  def publish(eventGenerator: Supplier[Event], every: Duration, onError: Consumer[PublishFailure]): Cancellable

  /**
   * Shuts down the connection for this publisher. Using any api of publisher after shutdown should give exceptions.
   * This method should be called while the component is shutdown gracefully.
   *
   * Any exception that occurs will cause the future to complete with a Failure.
   *
   * @return a future which completes when the underlying connection is shut down
   */
  private[event] def shutdown(): CompletableFuture[Done]
}
