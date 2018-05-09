package csw.services.event.internal.pubsub

import akka.actor.typed.ActorRef
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import csw.messages.events.{Event, EventKey}
import csw.services.event.internal.throttle.RateAdapterStage
import csw.services.event.scaladsl.{EventSubscriber, EventSubscription}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

abstract class BaseEventSubscriber extends EventSubscriber {

  implicit protected def mat: Materializer

  override def subscribe(eventKeys: Set[EventKey], every: FiniteDuration): Source[Event, EventSubscription] =
    subscribe(eventKeys).via(new RateAdapterStage[Event](every))

  override def subscribeAsync(eventKeys: Set[EventKey], callback: Event => Future[_]): EventSubscription =
    subscribe(eventKeys).mapAsync(1)(x => callback(x)).to(Sink.ignore).run()

  override def subscribeAsync(eventKeys: Set[EventKey], callback: Event => Future[_], every: FiniteDuration): EventSubscription =
    subscribe(eventKeys, every).mapAsync(1)(x => callback(x)).to(Sink.ignore).run()

  override def subscribeCallback(eventKeys: Set[EventKey], callback: Event => Unit): EventSubscription =
    subscribe(eventKeys).to(Sink.foreach(callback)).run()

  override def subscribeCallback(eventKeys: Set[EventKey], callback: Event => Unit, every: FiniteDuration): EventSubscription =
    subscribe(eventKeys, every).to(Sink.foreach(callback)).run()

  override def subscribeActorRef(eventKeys: Set[EventKey], actorRef: ActorRef[Event]): EventSubscription =
    subscribeCallback(eventKeys, event => actorRef ! event)

  override def subscribeActorRef(eventKeys: Set[EventKey], actorRef: ActorRef[Event], every: FiniteDuration): EventSubscription =
    subscribeCallback(eventKeys, event => actorRef ! event, every)
}
