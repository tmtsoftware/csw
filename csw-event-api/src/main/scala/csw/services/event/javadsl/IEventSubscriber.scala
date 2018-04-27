package csw.services.event.javadsl

import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

import akka.actor.typed.ActorRef
import akka.stream.Materializer
import akka.stream.javadsl.Source
import csw.messages.events.{Event, EventKey}
import csw.services.event.scaladsl.EventSubscriber

import scala.concurrent.duration.FiniteDuration

trait IEventSubscriber {

  def subscribe(eventKeys: java.util.Set[EventKey]): Source[Event, IEventSubscription]

  def subscribe(eventKeys: java.util.Set[EventKey], every: FiniteDuration): Source[Event, IEventSubscription]

  def subscribeAsync(
      eventKeys: java.util.Set[EventKey],
      callback: Event ⇒ CompletableFuture[_],
      mat: Materializer
  ): IEventSubscription

  def subscribeAsync(
      eventKeys: java.util.Set[EventKey],
      callback: Event => CompletableFuture[_],
      every: FiniteDuration,
      mat: Materializer
  ): IEventSubscription

  def subscribeCallback(eventKeys: java.util.Set[EventKey], callback: Consumer[Event], mat: Materializer): IEventSubscription

  def subscribeCallback(
      eventKeys: java.util.Set[EventKey],
      callback: Consumer[Event],
      every: FiniteDuration,
      mat: Materializer
  ): IEventSubscription

  def subscribeActorRef(eventKeys: java.util.Set[EventKey], actorRef: ActorRef[Event], mat: Materializer): IEventSubscription

  def subscribeActorRef(
      eventKeys: java.util.Set[EventKey],
      actorRef: ActorRef[Event],
      every: FiniteDuration,
      mat: Materializer
  ): IEventSubscription

  def get(eventKeys: java.util.Set[EventKey]): CompletableFuture[java.util.Set[Event]]

  def get(eventKey: EventKey): CompletableFuture[Event]

  def asScala: EventSubscriber
}
