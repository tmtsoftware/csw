package csw.services.event.scaladsl

import akka.actor.typed.ActorRef
import akka.stream.scaladsl.Source
import csw.messages.events.{Event, EventKey}
import csw.messages.params.models.Subsystem

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

trait EventSubscriber {

  // subscribe to multiple eventKeys and get a stream of events received
  def subscribe(eventKeys: Set[EventKey]): Source[Event, EventSubscription]

  // subscribe to multiple eventKeys and receive events at `every` frequency based on one of the given `mode` (RateAdapter or
  // RateLimiter)
  def subscribe(eventKeys: Set[EventKey], every: FiniteDuration, mode: SubscriptionMode): Source[Event, EventSubscription]

  // subscribe to multiple eventKeys and execute a future based callback for every event received. The callback is of event => future
  // type, so that blocking operation withing callback can be placed in the future (separate thread than main thread)
  def subscribeAsync(eventKeys: Set[EventKey], callback: Event => Future[_]): EventSubscription

  //subscribeAsync overload for receiving event at a `every` frequency based on one of the give `mode`
  def subscribeAsync(
      eventKeys: Set[EventKey],
      callback: Event => Future[_],
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription

  // subscribe to multiple events and execute the given callback for each received event. Note that any exception thrown from
  // `callback` is expected to be handled by component developers.
  def subscribeCallback(eventKeys: Set[EventKey], callback: Event => Unit): EventSubscription

  // subscribeCallback overload for receiving event at a `every` frequency based on one of the give `mode`
  def subscribeCallback(
      eventKeys: Set[EventKey],
      callback: Event => Unit,
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription

  // subscribe to multiple events and receive the event as actor message to given `actorRef`
  def subscribeActorRef(eventKeys: Set[EventKey], actorRef: ActorRef[Event]): EventSubscription

  // subscribeActorRef overload for receiving event at a `every` frequency based on one of the give `mode`
  def subscribeActorRef(
      eventKeys: Set[EventKey],
      actorRef: ActorRef[Event],
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription

  // pattern subscribe for a given subsystem and pattern, and get a stream of events received
  def pSubscribe(subsystem: Subsystem, pattern: String): Source[Event, EventSubscription]

  // pattern subscribe for a given subsystem and pattern, and execute a callback for each event received
  def pSubscribe(subsystem: Subsystem, pattern: String, callback: Event ⇒ Unit): EventSubscription

  // get events for multiple keys, if an event is not published for any eventKey, then invalid event is returned for that eventKey.
  // For all subscribe apis above, if for any eventKey no event is published then an invalid event is returned forr that eventKey.
  def get(eventKeys: Set[EventKey]): Future[Set[Event]]

  // get event for a single eventKey, if an event is not published for any eventKey, then invalid event is returned for that eventKey.
  def get(eventKey: EventKey): Future[Event]
}
