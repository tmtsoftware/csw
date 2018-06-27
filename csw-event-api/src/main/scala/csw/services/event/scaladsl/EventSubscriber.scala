package csw.services.event.scaladsl

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
   * Subscribe to multiple Event Keys and get a single stream of events for all event keys.
   *
   * @param eventKeys a set of [[csw.messages.events.EventKey]] to subscribe to
   * @return a [[akka.stream.scaladsl.Source]] of [[csw.messages.events.Event]]. The materialized value of the source provides an [[csw.services.event.scaladsl.EventSubscription]]
   *         which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribe(eventKeys: Set[EventKey]): Source[Event, EventSubscription]

  /**
   * Subscribe to multiple eventKeys and receive events at `every` frequency based on one of the given `mode` (RateAdapter or RateLimiter)
   *
   * @param eventKeys a set of [[csw.messages.events.EventKey]] to subscribe to
   * @param every the duration which determines the frequency with which events are received
   * @param mode an appropriate [[csw.services.event.scaladsl.SubscriptionMode]] to control the behavior of rate of events w.r.t. the given frequency.
   *             Refer the API documentation for SubscriptionMode for more details
   * @return a [[akka.stream.scaladsl.Source]] of [[csw.messages.events.Event]]. The materialized value of the source provides an [[csw.services.event.scaladsl.EventSubscription]]
   *         which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribe(eventKeys: Set[EventKey], every: FiniteDuration, mode: SubscriptionMode): Source[Event, EventSubscription]

  /**
   * Subscribes an asynchronous callback function to events from multiple eventKeys. The callback is of event => future
   * type, so that blocking operation within callback can be placed in the future (separate thread than main thread)
   *
   * @param eventKeys a set of [[csw.messages.events.EventKey]] to subscribe to
   * @param callback a function to execute asynchronously on each received event
   * @return an [[csw.services.event.scaladsl.EventSubscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribeAsync(eventKeys: Set[EventKey], callback: Event => Future[_]): EventSubscription

  /**
   * [[csw.services.event.scaladsl.EventSubscriber#subscribeAsync]] overload for receiving event at a `every` frequency based on one of the give `mode`
   *
   * @param eventKeys a set of [[csw.messages.events.EventKey]] to subscribe to
   * @param callback a function to execute on each received event
   * @param every the duration which determines the frequency with which events are received
   * @param mode an appropriate [[csw.services.event.scaladsl.SubscriptionMode]] to control the behavior of rate of events w.r.t. the given frequency.
   *             Refer the API documentation for SubscriptionMode for more details
   * @return an [[csw.services.event.scaladsl.EventSubscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribeAsync(
      eventKeys: Set[EventKey],
      callback: Event => Future[_],
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription

  /**
   * Subscribes a callback function to events from multiple event keys. Note that any exception thrown from `callback` is expected to be handled by
   * component developers.
   *
   * @param eventKeys a set of [[csw.messages.events.EventKey]] to subscribe to
   * @param callback a function to execute on each received event
   * @return an [[csw.services.event.scaladsl.EventSubscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribeCallback(eventKeys: Set[EventKey], callback: Event => Unit): EventSubscription

  /**
   * [[csw.services.event.scaladsl.EventSubscriber#subscribeCallback]] overload for receiving event at a `every` frequency based on one of the give `mode`
   *
   * @param eventKeys a set of [[csw.messages.events.EventKey]] to subscribe to
   * @param callback a function to execute on each received event
   * @param every the duration which determines the frequency with which events are received
   * @param mode an appropriate [[csw.services.event.scaladsl.SubscriptionMode]] to control the behavior of rate of events w.r.t. the given frequency.
   *             Refer the API documentation for SubscriptionMode for more details
   * @return an [[csw.services.event.scaladsl.EventSubscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribeCallback(
      eventKeys: Set[EventKey],
      callback: Event => Unit,
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription

  /**
   * Subscribes an actor to events from multiple event keys
   *
   * @param eventKeys a set of [[csw.messages.events.EventKey]] to subscribe to
   * @param actorRef an actorRef to an actor to which each received event is redirected
   * @return an [[csw.services.event.scaladsl.EventSubscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribeActorRef(eventKeys: Set[EventKey], actorRef: ActorRef[Event]): EventSubscription

  /**
   * [[csw.services.event.scaladsl.EventSubscriber#subscribeActorRef]] overload for receiving event at a `every` frequency based on one of the give `mode`
   *
   * @param eventKeys a set of [[csw.messages.events.EventKey]] to subscribe to
   * @param actorRef an actorRef to an actor to which each received event is redirected
   * @param every the duration which determines the frequency with which events are received
   * @param mode an appropriate [[csw.services.event.scaladsl.SubscriptionMode]] to control the behavior of rate of events w.r.t. the given frequency.
   *             Refer the API documentation for SubscriptionMode for more details
   * @return an [[csw.services.event.scaladsl.EventSubscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribeActorRef(
      eventKeys: Set[EventKey],
      actorRef: ActorRef[Event],
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription

  /**
   * Subscribe to events from Event Keys specified using a subsystem and a pattern to match the remaining Event Key
   *
   * @param subsystem a valid [[csw.messages.params.models.Subsystem]] which represents the source of the events
   * @param pattern   Subscribes the client to the given patterns. Supported glob-style patterns:
   *                  - h?llo subscribes to hello, hallo and hxllo
   *                  - h*llo subscribes to hllo and heeeello
   *                  - h[ae]llo subscribes to hello and hallo, but not hillo
   *                  Use \ to escape special characters if you want to match them verbatim.
   * @return a [[akka.stream.scaladsl.Source]] of [[csw.messages.events.Event]]. The materialized value of the source provides an [[csw.services.event.scaladsl.EventSubscription]]
   *         which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def pSubscribe(subsystem: Subsystem, pattern: String): Source[Event, EventSubscription]

  /**
   * Subscribes a callback to events from Event Keys specified using a subsystem and a pattern to match the remaining Event Key
   *
   * @param subsystem a valid [[csw.messages.params.models.Subsystem]] which represents the source of the events
   * @param pattern   Subscribes the client to the given patterns. Supported glob-style patterns:
                      - h?llo subscribes to hello, hallo and hxllo
                      - h*llo subscribes to hllo and heeeello
                      - h[ae]llo subscribes to hello and hallo, but not hillo
                      Use \ to escape special characters if you want to match them verbatim.
   * @param callback   a function to execute on each received event
   * @return an [[csw.services.event.scaladsl.EventSubscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def pSubscribeCallback(subsystem: Subsystem, pattern: String, callback: Event ⇒ Unit): EventSubscription

  /**
   * Get latest events for multiple Event Keys. If an event is not published for any Event Key, then `invalid event` is returned for that Event Key.
   * For all subscribe APIs above, if for any eventKey no event is published then an invalid event is returned for that Event Key.
   *
   * @param eventKeys a set of [[csw.messages.events.EventKey]] to subscribe to
   * @return a future which completes with a set of latest [[csw.messages.events.Event]] for the provided Event Keys
   */
  def get(eventKeys: Set[EventKey]): Future[Set[Event]]

  /**
   * Get latest event for the given Event Key. If an event is not published for any eventKey, then `invalid event` is returned for that Event Key.
   *
   * @param eventKey an [[csw.messages.events.EventKey]] to subscribe to
   * @return a future which completes with the latest [[csw.messages.events.Event]] for the provided Event Key
   */
  def get(eventKey: EventKey): Future[Event]
}
