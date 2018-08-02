package csw.services.event.api.scaladsl

import akka.actor.typed.ActorRef
import akka.stream.scaladsl.Source
import csw.messages.events.{Event, EventKey}
import csw.messages.params.models.Subsystem

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
 * An EventSubscriber interface to subscribe events. The events can be subscribed on [[csw.messages.events.EventKey]]. All events published on this key
 * will be received by subscribers.
 */
trait EventSubscriber {

  /**
   * Subscribe to multiple Event Keys and get a single stream of events for all event keys. The latest events available for the given
   * Event Keys will be received first. If event is not published for one or more event keys, `invalid event` will be received for those Event Keys.
   *
   * At the time of invocation, in case the underlying server is not available [[csw.services.event.api.exceptions.EventServerNotAvailable]] exception is thrown
   * and the stream is stopped after logging appropriately. In all other cases of exception, as per the default behavior, the stream will stop.
   * To avoid that, user should provide a resuming materializer while running the stream.
   *
    @note All the other APIs of [[EventSubscriber]] that do not return a [[akka.stream.scaladsl.Source]], internally use the resuming materializer which will ignore the failed event and resume receiving further events.
   *
   * @param eventKeys a set of [[csw.messages.events.EventKey]] to subscribe to
   * @return a [[akka.stream.scaladsl.Source]] of [[csw.messages.events.Event]]. The materialized value of the source provides an [[csw.services.event.api.scaladsl.EventSubscription]]
   *         which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribe(eventKeys: Set[EventKey]): Source[Event, EventSubscription]

  /**
   * Subscribe to multiple eventKeys and receive events at `every` frequency according to the specified `mode` (RateAdapter or RateLimiter). The latest events available for the given
   * Event Keys will be received first. If event is not published for one or more event keys, `invalid event` will be received for those Event Keys.
   *
   * At the time of invocation, in case the underlying server is not available, [[csw.services.event.api.exceptions.EventServerNotAvailable]] exception is thrown
   * and the stream is stopped after logging appropriately. In all other cases of exception, as per the default behavior, the stream will stop.
   * To avoid that, user should provide a resuming materializer while running the stream.
   *
   * @note All the other APIs of [[EventSubscriber]] that do not return a [[akka.stream.scaladsl.Source]], internally use the resuming materializer which will ignore the failed event and resume receiving further events.
   *
   * @param eventKeys a set of [[csw.messages.events.EventKey]] to subscribe to
   * @param every the duration which determines the frequency with which events are received
   * @param mode an appropriate [[csw.services.event.api.scaladsl.SubscriptionMode]] to control the behavior of rate of events w.r.t. the given frequency.
   *             Refer the API documentation for SubscriptionMode for more details
   * @return a [[akka.stream.scaladsl.Source]] of [[csw.messages.events.Event]]. The materialized value of the source provides an
   *         [[csw.services.event.api.scaladsl.EventSubscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribe(eventKeys: Set[EventKey], every: FiniteDuration, mode: SubscriptionMode): Source[Event, EventSubscription]

  /**
   * Subscribes an asynchronous callback function to events from multiple eventKeys. The callback is of event => future
   * type, so that blocking operation within callback can be placed in the future (separate thread than main thread). The latest events available for the given
   * Event Keys will be received first. If event is not published for one or more event keys, `invalid event` will be received for those Event Keys.
   *
   * At the time of invocation, in case the underlying server is not available, [[csw.services.event.api.exceptions.EventServerNotAvailable]] exception is thrown
   * and the subscription is stopped after logging appropriately. [[csw.services.event.api.scaladsl.EventSubscription!.ready]] method can be used to determine
   * this state. In all other cases of exception, the subscription resumes to receive remaining elements.
   *
   * @param eventKeys a set of [[csw.messages.events.EventKey]] to subscribe to
   * @param callback a function to execute asynchronously on each received event
   * @return an [[csw.services.event.api.scaladsl.EventSubscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribeAsync(eventKeys: Set[EventKey], callback: Event => Future[_]): EventSubscription

  /**
   * Overload for above `subscribeAsync` for receiving event at a `every` frequency according to the specified `mode`. The latest events available for the given
   * Event Keys will be received first. If event is not published for one or more event keys, `invalid event` will be received for those Event Keys.
   *
   * At the time of invocation, in case the underlying server is not available, [[csw.services.event.api.exceptions.EventServerNotAvailable]] exception is thrown
   * and the subscription is stopped after logging appropriately. [[csw.services.event.api.scaladsl.EventSubscription!.ready]] method can be used to determine
   * this state. In all other cases of exception, the subscription resumes to receive remaining elements.
   *
   * @param eventKeys a set of [[csw.messages.events.EventKey]] to subscribe to
   * @param callback a function to execute on each received event
   * @param every the duration which determines the frequency with which events are received
   * @param mode an appropriate [[csw.services.event.api.scaladsl.SubscriptionMode]] to control the behavior of rate of events w.r.t. the given frequency.
   *             Refer the API documentation for SubscriptionMode for more details
   * @return an [[csw.services.event.api.scaladsl.EventSubscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribeAsync(
      eventKeys: Set[EventKey],
      callback: Event => Future[_],
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription

  /**
   * Subscribes a callback function to events from multiple event keys. Note that any exception thrown from `callback` is expected to be handled by
   * component developers. The latest events available for the given Event Keys will be received first. If event is not published for one or more event keys,
   * `invalid event` will be received for those Event Keys.
   *
   * At the time of invocation, in case the underlying server is not available, [[csw.services.event.api.exceptions.EventServerNotAvailable]] exception is thrown
   * and the subscription is stopped after logging appropriately. [[csw.services.event.api.scaladsl.EventSubscription!.ready]] method can be used to determine this
   * state. In all other cases of exception, the subscription resumes to receive remaining elements.
   *
   * @param eventKeys a set of [[csw.messages.events.EventKey]] to subscribe to
   * @param callback a function to execute on each received event
   * @return an [[csw.services.event.api.scaladsl.EventSubscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribeCallback(eventKeys: Set[EventKey], callback: Event => Unit): EventSubscription

  /**
   * Overload for above `subscribeCallback` for receiving event at a `every` frequency according to the specified `mode`. The latest events available for the given
   * Event Keys will be received first. If event is not published for one or more event keys, `invalid event` will be received for those Event Keys.
   *
   * At the time of invocation, in case the underlying server is not available, [[csw.services.event.api.exceptions.EventServerNotAvailable]] exception is thrown
   * and the subscription is stopped after logging appropriately. [[csw.services.event.api.scaladsl.EventSubscription!.ready]] method can be used to determine this
   * state. In all other cases of exception, the subscription resumes to receive remaining elements.
   *
   * @param eventKeys a set of [[csw.messages.events.EventKey]] to subscribe to
   * @param callback a function to execute on each received event
   * @param every the duration which determines the frequency with which events are received
   * @param mode an appropriate [[csw.services.event.api.scaladsl.SubscriptionMode]] to control the behavior of rate of events w.r.t. the given frequency.
   *             Refer the API documentation for SubscriptionMode for more details
   * @return an [[csw.services.event.api.scaladsl.EventSubscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribeCallback(
      eventKeys: Set[EventKey],
      callback: Event => Unit,
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription

  /**
   * Subscribes an actor to events from multiple event keys. The latest events available for the given
   * Event Keys will be received first. If event is not published for one or more event keys, `invalid event` will be received for those Event Keys.
   *
   * At the time of invocation, in case the underlying server is not available, [[csw.services.event.api.exceptions.EventServerNotAvailable]] exception is thrown
   * and the subscription is stopped after logging appropriately. [[csw.services.event.api.scaladsl.EventSubscription!.ready]] method can be used to determine this
   * state. In all other cases of exception, the subscription resumes to receive remaining elements.
   *
   * @param eventKeys a set of [[csw.messages.events.EventKey]] to subscribe to
   * @param actorRef an actorRef of an actor which handles each received event
   * @return an [[csw.services.event.api.scaladsl.EventSubscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribeActorRef(eventKeys: Set[EventKey], actorRef: ActorRef[Event]): EventSubscription

  /**
   * Overload for above `subscribeActorRef` for receiving event at a `every` frequency according to the specified `mode`. The latest events available for the given
   * Event Keys will be received first. If event is not published for one or more event keys, `invalid event` will be received for those Event Keys.
   *
   * At the time of invocation, in case the underlying server is not available, [[csw.services.event.api.exceptions.EventServerNotAvailable]] exception is thrown
   * and the subscription is stopped after logging appropriately. [[csw.services.event.api.scaladsl.EventSubscription!.ready]] method can be used to determine this
   * state. In all other cases of exception, the subscription resumes to receive remaining elements.
   *
   * @param eventKeys a set of [[csw.messages.events.EventKey]] to subscribe to
   * @param actorRef an actorRef of an actor to which each received event is redirected
   * @param every the duration which determines the frequency with which events are received
   * @param mode an appropriate [[csw.services.event.api.scaladsl.SubscriptionMode]] to control the behavior of rate of events w.r.t. the given frequency.
   *             Refer the API documentation for SubscriptionMode for more details
   * @return an [[csw.services.event.api.scaladsl.EventSubscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribeActorRef(
      eventKeys: Set[EventKey],
      actorRef: ActorRef[Event],
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription

  /**
   * Subscribe to events from Event Keys specified using a subsystem and a pattern to match the remaining Event Key. The latest events available for the given
   * Event Keys will be received first. If event is not published for one or more event keys, `invalid event` will be received for those Event Keys.
   *
   * At the time of invocation, in case the underlying server is not available, [[csw.services.event.api.exceptions.EventServerNotAvailable]] exception is thrown
   * and the subscription is stopped after logging appropriately. [[csw.services.event.api.scaladsl.EventSubscription!.ready]] method can be used to determine this
   * state. In all other cases of exception, the subscription resumes to receive remaining elements.
   *
   * @param subsystem a valid [[csw.messages.params.models.Subsystem]] which represents the source of the events
   * @param pattern   Subscribes the client to the given patterns. Supported glob-style patterns:
   *                  - h?llo subscribes to hello, hallo and hxllo
   *                  - h*llo subscribes to hllo and heeeello
   *                  - h[ae]llo subscribes to hello and hallo, but not hillo
   *                  Use \ to escape special characters if you want to match them verbatim.
   * @return a [[akka.stream.scaladsl.Source]] of [[csw.messages.events.Event]]. The materialized value of the source provides an [[csw.services.event.api.scaladsl.EventSubscription]]
   *         which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def pSubscribe(subsystem: Subsystem, pattern: String): Source[Event, EventSubscription]

  /**
   * Subscribes a callback to events from Event Keys specified using a subsystem and a pattern to match the remaining Event Key. The latest events available for the given
   * Event Keys will be received first. If event is not published for one or more event keys, `invalid event` will be received for those Event Keys.
   *
   * At the time of invocation, in case the underlying server is not available, [[csw.services.event.api.exceptions.EventServerNotAvailable]] exception is thrown
   * and the subscription is stopped after logging appropriately. [[csw.services.event.api.scaladsl.EventSubscription!.ready]] method can be used to determine this
   * state. In all other cases of exception, the subscription resumes to receive remaining elements.
   *
   * @param subsystem a valid [[csw.messages.params.models.Subsystem]] which represents the source of the events
   * @param pattern   Subscribes the client to the given patterns. Supported glob-style patterns:
   *- h?llo subscribes to hello, hallo and hxllo
   *- h*llo subscribes to hllo and heeeello
   *- h[ae]llo subscribes to hello and hallo, but not hillo
   *                 Use \ to escape special characters if you want to match them verbatim.
   * @param callback  a function to execute on each received event
   * @return an [[csw.services.event.api.scaladsl.EventSubscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def pSubscribeCallback(subsystem: Subsystem, pattern: String, callback: Event ⇒ Unit): EventSubscription

  /**
   * Get latest events for multiple Event Keys. The latest events available for the given Event Keys will be received first.
   * If event is not published for one or more event keys, `invalid event` will be received for those Event Keys.
   *
   * In case the underlying server is not available, the future fails with [[csw.services.event.api.exceptions.EventServerNotAvailable]] exception.
   * In all other cases of exception, the future fails with the respective exception
   *
   * @param eventKeys a set of [[csw.messages.events.EventKey]] to subscribe to
   * @return a future which completes with a set of latest [[csw.messages.events.Event]] for the provided Event Keys
   */
  def get(eventKeys: Set[EventKey]): Future[Set[Event]]

  /**
   * Get latest event for the given Event Key. If an event is not published for any eventKey, then `invalid event` is returned for that Event Key.
   *
   * In case the underlying server is not available, the future fails with [[csw.services.event.api.exceptions.EventServerNotAvailable]] exception.
   * In all other cases of exception, the future fails with the respective exception
   *
   * @param eventKey an [[csw.messages.events.EventKey]] to subscribe to
   * @return a future which completes with the latest [[csw.messages.events.Event]] for the provided Event Key
   */
  def get(eventKey: EventKey): Future[Event]
}
