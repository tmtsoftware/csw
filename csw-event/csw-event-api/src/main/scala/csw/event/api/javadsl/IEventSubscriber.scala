package csw.event.api.javadsl

import java.time.Duration
import java.util
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

import akka.actor.typed.ActorRef
import akka.stream.javadsl.Source
import csw.event.api.scaladsl.SubscriptionMode
import csw.params.core.models.Subsystem
import csw.params.events.{Event, EventKey}

/**
 * An EventSubscriber interface to subscribe events. The events can be subscribed on [[csw.params.events.EventKey]]. All events
 * published on this key will be received by subscribers.
 */
trait IEventSubscriber {

  /**
   * Subscribe to multiple Event Keys and get a single stream of events for all event keys. The latest events available for the given
   * Event Keys will be received first. If event is not published for one or more event keys, `invalid event` will be received for those Event Keys.
   *
   * At the time of invocation, in case the underlying server is not available [[csw.event.api.exceptions.EventServerNotAvailable]] exception is thrown
   * and the stream is stopped after logging appropriately. In all other cases of exception, the stream resumes to receive remaining elements.
   *
   * @param eventKeys a set of [[csw.params.events.EventKey]] to subscribe to
   * @return a [[akka.stream.javadsl.Source]] of [[csw.params.events.Event]]. The materialized value of the source provides an
   *         [[csw.event.api.javadsl.IEventSubscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribe(eventKeys: util.Set[EventKey]): Source[Event, IEventSubscription]

  /**
   * Subscribe to multiple eventKeys and receive events at `every` frequency according to the specified given `mode` (RateAdapter or RateLimiter). The latest events
   * available for the given Event Keys will be received first. If event is not published for one or more event keys, `invalid event` will be received for
   * those Event Keys.
   *
   * At the time of invocation, in case the underlying server is not available, [[csw.event.api.exceptions.EventServerNotAvailable]] exception is thrown
   * and the stream is stopped after logging appropriately. In all other cases of exception, the stream resumes to receive remaining elements.
   *
   * @param eventKeys a set of [[csw.params.events.EventKey]] to subscribe to
   * @param every the duration which determines the frequency with which events are received
   * @param mode an appropriate [[csw.event.api.scaladsl.SubscriptionMode]] to control the behavior of rate of events w.r.t. the given frequency.
   *             Refer the API documentation for SubscriptionMode for more details
   * @return a [[akka.stream.javadsl.Source]] of [[csw.params.events.Event]]. The materialized value of the source provides an
   *         [[csw.event.api.javadsl.IEventSubscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribe(eventKeys: util.Set[EventKey], every: Duration, mode: SubscriptionMode): Source[Event, IEventSubscription]

  /**
   *
   * Subscribes an asynchronous callback function to events from multiple eventKeys. The callback is of type event => future
   * and it ensures that the event callbacks are called sequentially in such a way that the subsequent execution will
   * start only after the prior one completes. This API gives the guarantee of ordered execution of the asynchronous callbacks.
   *
   * The latest events available for the given Event Keys will be received first.
   * If event is not published for one or more event keys, `invalid event` will be received for those Event Keys.
   *
   * At the time of invocation, in case the underlying server is not available, [[csw.event.api.exceptions.EventServerNotAvailable]] exception is thrown
   * and the subscription is stopped after logging appropriately. [[csw.event.api.scaladsl.EventSubscription!.ready]] method can be used to determine
   * this state. In all other cases of exception, the subscription resumes to receive remaining elements.
   *
   * Note that callbacks are not thread-safe on the JVM. If you need to do side effects/mutations, prefer using [[subscribeActorRef]] API.
   *
   * @param eventKeys a set of [[csw.params.events.EventKey]] to subscribe to
   * @param callback a function to execute asynchronously on each received event
   * @return an [[csw.event.api.javadsl.IEventSubscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribeAsync(eventKeys: util.Set[EventKey], callback: Event ⇒ CompletableFuture[_]): IEventSubscription

  /**
   * Overload for above `subscribeAsync` for receiving event at a `every` frequency according to the specified `mode`. The latest
   * events available for the given Event Keys will be received first. If event is not published for one or more event keys, `invalid event` will be received for those Event Keys.
   *
   * At the time of invocation, in case the underlying server is not available, [[csw.event.api.exceptions.EventServerNotAvailable]] exception is thrown
   * and the subscription is stopped after logging appropriately. [[csw.event.api.scaladsl.EventSubscription!.ready]] method can be used to determine
   * this state. In all other cases of exception, the subscription resumes to receive remaining elements.
   *
   * @param eventKeys a set of [[csw.params.events.EventKey]] to subscribe to
   * @param callback a function to execute on each received event
   * @param every the duration which determines the frequency with which events are received
   * @param mode an appropriate [[csw.event.api.scaladsl.SubscriptionMode]] to control the behavior of rate of events w.r.t. the given frequency.
   *             Refer the API documentation for SubscriptionMode for more details
   * @return an [[csw.event.api.javadsl.IEventSubscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribeAsync(
      eventKeys: util.Set[EventKey],
      callback: Event ⇒ CompletableFuture[_],
      every: Duration,
      mode: SubscriptionMode
  ): IEventSubscription

  /**
   * Subscribes a callback function to events from multiple event keys. The latest events available for the given Event Keys will be received first.
   * If event is not published for one or more event keys, `invalid event` will be received for those Event Keys.
   *
   * At the time of invocation, in case the underlying server is not available, [[csw.event.api.exceptions.EventServerNotAvailable]] exception is thrown
   * and the subscription is stopped after logging appropriately. [[csw.event.api.scaladsl.EventSubscription!.ready]] method can be used to determine this
   * state. In all other cases of exception, the subscription resumes to receive remaining elements.
   *
   * Note that callbacks are not thread-safe on the JVM. If you need to do side effects/mutations, prefer using [[subscribeActorRef]] API.
   * Also note that any exception thrown from `callback` is expected to be handled by component developers.
   *
   * @param eventKeys a set of [[csw.params.events.EventKey]] to subscribe to
   * @param callback a consumer which defines an operation to execute on each received event
   * @return an [[csw.event.api.javadsl.IEventSubscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribeCallback(eventKeys: util.Set[EventKey], callback: Consumer[Event]): IEventSubscription

  /**
   * Overload for above `subscribeCallback` for receiving event at a `every` frequency according to the specified `mode`.
   * The latest events available for the given Event Keys will be received first. If event is not published for one or more event keys, `invalid event` will
   * be received for those Event Keys.
   *
   * At the time of invocation, in case the underlying server is not available, [[csw.event.api.exceptions.EventServerNotAvailable]] exception is thrown
   * and the subscription is stopped after logging appropriately. [[csw.event.api.scaladsl.EventSubscription!.ready]] method can be used to determine this
   * state. In all other cases of exception, the subscription resumes to receive remaining elements.
   *
   * @param eventKeys a set of [[csw.params.events.EventKey]] to subscribe to
   * @param callback a consumer which defines an operation to execute on each received event
   * @param every the duration which determines the frequency with which events are received
   * @param mode an appropriate [[csw.event.api.scaladsl.SubscriptionMode]] to control the behavior of rate of events w.r.t. the given frequency.
   *             Refer the API documentation for SubscriptionMode for more details
   * @return an [[csw.event.api.javadsl.IEventSubscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribeCallback(
      eventKeys: util.Set[EventKey],
      callback: Consumer[Event],
      every: Duration,
      mode: SubscriptionMode
  ): IEventSubscription

  /**
   * Subscribes an actor to events from multiple event keys. The latest events available for the given Event Keys will be received first.
   * If event is not published for one or more event keys, `invalid event` will be received for those Event Keys.
   *
   * At the time of invocation, in case the underlying server is not available, [[csw.event.api.exceptions.EventServerNotAvailable]] exception is thrown
   * and the subscription is stopped after logging appropriately. [[csw.event.api.scaladsl.EventSubscription!.ready]] method can be used to determine this
   * state. In all other cases of exception, the subscription resumes to receive remaining elements.
   *
   * @param eventKeys a set of [[csw.params.events.EventKey]] to subscribe to
   * @param actorRef an actorRef of an actor which handles each received event
   * @return an [[csw.event.api.javadsl.IEventSubscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribeActorRef(eventKeys: util.Set[EventKey], actorRef: ActorRef[Event]): IEventSubscription

  /**
   * Overload for above `subscribeActorRef` for receiving event at a `every` frequency according to the specified give `mode`.
   * The latest events available for the given Event Keys will be received first. If event is not published for one or more event keys, `invalid event` will be
   * received for those Event Keys.
   *
   * At the time of invocation, in case the underlying server is not available, [[csw.event.api.exceptions.EventServerNotAvailable]] exception is thrown
   * and the subscription is stopped after logging appropriately. [[csw.event.api.scaladsl.EventSubscription!.ready]] method can be used to determine this
   * state. In all other cases of exception, the subscription resumes to receive remaining elements.
   *
   * @param eventKeys a set of [[csw.params.events.EventKey]] to subscribe to
   * @param actorRef an actorRef of an actor to which each received event is redirected
   * @param every the duration which determines the frequency with which events are received
   * @param mode an appropriate [[csw.event.api.scaladsl.SubscriptionMode]] to control the behavior of rate of events w.r.t. the given frequency.
   *             Refer the API documentation for SubscriptionMode for more details
   * @return an [[csw.event.api.javadsl.IEventSubscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribeActorRef(
      eventKeys: util.Set[EventKey],
      actorRef: ActorRef[Event],
      every: Duration,
      mode: SubscriptionMode
  ): IEventSubscription

  /**
   * Subscribe to events from Event Keys specified using a subsystem and a pattern to match the remaining Event Key
   *
   * At the time of invocation, in case the underlying server is not available, [[csw.event.api.exceptions.EventServerNotAvailable]] exception is thrown
   * and the subscription is stopped after logging appropriately. [[csw.event.api.scaladsl.EventSubscription!.ready]] method can be used to determine this
   * state. In all other cases of exception, the subscription resumes to receive remaining elements.
   *
   * @param subsystem a valid [[csw.params.core.models.Subsystem]] which represents the source of the events
   * @param pattern   Subscribes the client to the given patterns. Supported glob-style patterns:
   *                  - h?llo subscribes to hello, hallo and hxllo
   *                  - h*llo subscribes to hllo and heeeello
   *                  - h[ae]llo subscribes to hello and hallo, but not hillo
   *                  Use \ to escape special characters if you want to match them verbatim.
   * @return a [[akka.stream.javadsl.Source]] of [[csw.params.events.Event]]. The materialized value of the source provides an [[csw.event.api.javadsl.IEventSubscription]]
   *         which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def pSubscribe(subsystem: Subsystem, pattern: String): Source[Event, IEventSubscription]

  /**
   * Subscribes a callback to events from Event Keys specified using a subsystem and a pattern to match the remaining Event Key
   *
   * At the time of invocation, in case the underlying server is not available, [[csw.event.api.exceptions.EventServerNotAvailable]] exception is thrown
   * and the subscription is stopped after logging appropriately. [[csw.event.api.scaladsl.EventSubscription!.ready]] method can be used to determine this
   * state. In all other cases of exception, the subscription resumes to receive remaining elements.
   *
   * Note that callbacks are not thread-safe on the JVM. If you are doing side effects/mutations inside the callback, you should ensure that it is done in a thread-safe way inside an actor.
   *
   * @param subsystem a valid [[csw.params.core.models.Subsystem]] which represents the source of the events
   * @param pattern   Subscribes the client to the given patterns. Supported glob-style patterns:
   *- h?llo subscribes to hello, hallo and hxllo
   *- h*llo subscribes to hllo and heeeello
   *- h[ae]llo subscribes to hello and hallo, but not hillo
   *Use \ to escape special characters if you want to match them verbatim.
   * @param callback a consumer which defines an operation to execute on each received event
   * @return an [[csw.event.api.javadsl.IEventSubscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def pSubscribeCallback(subsystem: Subsystem, pattern: String, callback: Consumer[Event]): IEventSubscription

  /**
   * Get latest events for multiple Event Keys. The latest events available for the given Event Keys will be received first.
   * If event is not published for one or more event keys, `invalid event` will be received for those Event Keys.
   *
   * In case the underlying server is not available, the future fails with [[csw.event.api.exceptions.EventServerNotAvailable]] exception.
   * In all other cases of exception, the future fails with the respective exception
   *
   * @param eventKeys a set of [[csw.params.events.EventKey]] to subscribe to
   * @return a completable future which completes with a set of latest [[csw.params.events.Event]] for the provided Event Keys
   */
  def get(eventKeys: util.Set[EventKey]): CompletableFuture[util.Set[Event]]

  /**
   * Get latest event for the given Event Key. If an event is not published for any eventKey, then `invalid event` is returned for that Event Key.
   *
   * In case the underlying server is not available, the future fails with [[csw.event.api.exceptions.EventServerNotAvailable]] exception.
   * In all other cases of exception, the future fails with the respective exception
   *
   * @param eventKey an [[csw.params.events.EventKey]] to subscribe to
   * @return a completable future which completes with the latest [[csw.params.events.Event]] for the provided Event Key
   */
  def get(eventKey: EventKey): CompletableFuture[Event]
}
