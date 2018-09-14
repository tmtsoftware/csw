package csw.event.internal.commons

import akka.actor.typed.ActorRef
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.stage.GraphStage
import akka.stream.{FlowShape, Materializer}
import csw.params.events.Event
import csw.event.api.scaladsl.{EventSubscription, SubscriptionMode}
import csw.event.api.scaladsl.SubscriptionModes.{RateAdapterMode, RateLimiterMode}
import csw.event.internal.commons.throttle.{RateAdapterStage, RateLimiterStage}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
 * Utility class to provided common functionalities to different implementations of EventSubscriber
 */
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

  def pSubscribe(stream: Source[Event, EventSubscription], callback: Event ⇒ Unit): EventSubscription =
    stream.toMat(Sink.foreach(callback))(Keep.left).run()
}
