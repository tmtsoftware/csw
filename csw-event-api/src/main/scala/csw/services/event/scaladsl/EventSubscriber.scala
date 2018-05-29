package csw.services.event.scaladsl

import akka.actor.typed.ActorRef
import akka.stream.scaladsl.Source
import csw.messages.events.{Event, EventKey}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

trait EventSubscriber {

  def subscribe(eventKeys: Set[EventKey]): Source[Event, EventSubscription]

  def subscribe(eventKeys: Set[EventKey], every: FiniteDuration, mode: SubscriptionMode): Source[Event, EventSubscription]

  def subscribeAsync(eventKeys: Set[EventKey], callback: Event => Future[_]): EventSubscription

  def subscribeAsync(
      eventKeys: Set[EventKey],
      callback: Event => Future[_],
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription

  def subscribeCallback(eventKeys: Set[EventKey], callback: Event => Unit): EventSubscription

  def subscribeCallback(
      eventKeys: Set[EventKey],
      callback: Event => Unit,
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription

  def subscribeActorRef(eventKeys: Set[EventKey], actorRef: ActorRef[Event]): EventSubscription

  def subscribeActorRef(
      eventKeys: Set[EventKey],
      actorRef: ActorRef[Event],
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription

  def pSubscribe(pattern: Set[String]): Source[Event, EventSubscription]

  def get(eventKeys: Set[EventKey]): Future[Set[Event]]

  def get(eventKey: EventKey): Future[Event]
}
