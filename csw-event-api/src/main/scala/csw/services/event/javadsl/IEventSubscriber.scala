package csw.services.event.javadsl

import java.time.Duration
import java.util
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

import akka.actor.typed.ActorRef
import akka.stream.Materializer
import akka.stream.javadsl.Source
import csw.messages.events.{Event, EventKey}
import csw.messages.params.models.Subsystem
import csw.services.event.scaladsl.SubscriptionMode

trait IEventSubscriber {

  def subscribe(eventKeys: util.Set[EventKey]): Source[Event, IEventSubscription]

  def subscribe(eventKeys: util.Set[EventKey], every: Duration, mode: SubscriptionMode): Source[Event, IEventSubscription]

  def subscribeAsync(eventKeys: util.Set[EventKey], callback: Event ⇒ CompletableFuture[_]): IEventSubscription

  def subscribeAsync(
      eventKeys: util.Set[EventKey],
      callback: Event ⇒ CompletableFuture[_],
      every: Duration,
      mode: SubscriptionMode
  ): IEventSubscription

  def subscribeCallback(eventKeys: util.Set[EventKey], callback: Consumer[Event]): IEventSubscription

  def subscribeCallback(
      eventKeys: util.Set[EventKey],
      callback: Consumer[Event],
      every: Duration,
      mode: SubscriptionMode
  ): IEventSubscription

  def subscribeActorRef(eventKeys: util.Set[EventKey], actorRef: ActorRef[Event], mat: Materializer): IEventSubscription

  def subscribeActorRef(
      eventKeys: util.Set[EventKey],
      actorRef: ActorRef[Event],
      every: Duration,
      mode: SubscriptionMode
  ): IEventSubscription

  def pSubscribe(subsystem: Subsystem, pattern: String): Source[Event, IEventSubscription]

  def pSubscribe(subsystem: Subsystem, pattern: String, callback: Consumer[Event]): IEventSubscription

  def get(eventKeys: util.Set[EventKey]): CompletableFuture[util.Set[Event]]

  def get(eventKey: EventKey): CompletableFuture[Event]
}
