package csw.services.event.internal.pubsub

import akka.actor.typed.ActorRef
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import csw.messages.events.{Event, EventKey}
import csw.services.event.internal.throttle.{RateAdapterStage, RateLimiterStage}
import csw.services.event.scaladsl.SubscriptionMode.{RateAdapterMode, RateLimiterMode}
import csw.services.event.scaladsl.{EventSubscriber, EventSubscription, SubscriptionMode}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

abstract class AbstractEventSubscriber extends EventSubscriber {

  implicit protected def mat: Materializer

  override def subscribe(
      eventKeys: Set[EventKey],
      every: FiniteDuration,
      mode: SubscriptionMode
  ): Source[Event, EventSubscription] = subscribe(eventKeys).via {
    mode match {
      case RateAdapterMode => new RateAdapterStage[Event](every)
      case RateLimiterMode => new RateLimiterStage[Event](every)
    }
  }

  override def subscribeAsync(eventKeys: Set[EventKey], callback: Event => Future[_]): EventSubscription =
    subscribe(eventKeys).mapAsync(1)(x => callback(x)).to(Sink.ignore).run()

  override def subscribeAsync(
      eventKeys: Set[EventKey],
      callback: Event => Future[_],
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription = subscribe(eventKeys, every, mode).mapAsync(1)(x => callback(x)).to(Sink.ignore).run()

  override def subscribeCallback(eventKeys: Set[EventKey], callback: Event => Unit): EventSubscription =
    subscribe(eventKeys).to(Sink.foreach(callback)).run()

  override def subscribeCallback(
      eventKeys: Set[EventKey],
      callback: Event => Unit,
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription = subscribe(eventKeys, every, mode).to(Sink.foreach(callback)).run()

  override def subscribeActorRef(eventKeys: Set[EventKey], actorRef: ActorRef[Event]): EventSubscription =
    subscribeCallback(eventKeys, event => actorRef ! event)

  override def subscribeActorRef(
      eventKeys: Set[EventKey],
      actorRef: ActorRef[Event],
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription = subscribeCallback(eventKeys, event => actorRef ! event, every, mode)
}
