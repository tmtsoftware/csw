package csw.services.event.internal.pubsub

import akka.actor.typed.ActorRef
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.stage.GraphStage
import akka.stream.{FlowShape, Materializer}
import csw.messages.events.Event
import csw.services.event.internal.throttle.{RateAdapterStage, RateLimiterStage}
import csw.services.event.scaladsl.SubscriptionMode.{RateAdapterMode, RateLimiterMode}
import csw.services.event.scaladsl.{EventSubscription, SubscriptionMode}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class EventSubscriberUtil(implicit mat: Materializer) {

  def subscriptionModeStage(
      every: FiniteDuration,
      mode: SubscriptionMode
  ): GraphStage[FlowShape[Event, Event]] = mode match {
    case RateAdapterMode => new RateAdapterStage[Event](every)
    case RateLimiterMode => new RateLimiterStage[Event](every)
  }

  def subscribeAsync(eventSource: Source[Event, EventSubscription], callback: Event => Future[_]): EventSubscription =
    eventSource.mapAsync(1)(x => callback(x)).to(Sink.ignore).run()

  def subscribeCallback(eventSource: Source[Event, EventSubscription], callback: Event => Unit): EventSubscription =
    eventSource.to(Sink.foreach(callback)).run()

  def actorCallback(actorRef: ActorRef[Event]): Event ⇒ Unit = event ⇒ actorRef ! event
}
