package csw.services.event.javadsl

import java.util.concurrent.CompletableFuture

import akka.actor.typed.ActorRef
import akka.stream.Materializer
import akka.stream.javadsl.Source
import akka.stream.scaladsl.Sink
import csw.messages.events.{Event, EventKey}
import csw.services.event.internal.RateAdapterStage

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.duration.FiniteDuration

trait IEventSubscriber {

  def subscribe(eventKeys: java.util.Set[EventKey]): Source[Event, IEventSubscription]

  def subscribe(eventKeys: java.util.Set[EventKey], every: FiniteDuration): Source[Event, IEventSubscription] = {
    subscribe(eventKeys).via(new RateAdapterStage[Event](every))
  }

  def subscribeAsync(
      eventKeys: java.util.Set[EventKey],
      callback: Event ⇒ CompletableFuture[_],
      mat: Materializer
  ): IEventSubscription = {
    subscribe(eventKeys).asScala.mapAsync(1)(x => callback(x).toScala).to(Sink.ignore).run()(mat)
  }

  def subscribeAsync(
      eventKeys: java.util.Set[EventKey],
      callback: Event => CompletableFuture[_],
      every: FiniteDuration,
      mat: Materializer
  ): IEventSubscription = {
    subscribe(eventKeys, every).asScala.mapAsync(1)(x => callback(x).toScala).to(Sink.ignore).run()(mat)
  }

  def subscribeCallback(eventKeys: java.util.Set[EventKey], callback: Event => Unit, mat: Materializer): IEventSubscription = {
    subscribe(eventKeys).asScala.to(Sink.foreach(callback)).run()(mat)
  }

  def subscribeCallback(
      eventKeys: java.util.Set[EventKey],
      callback: Event => Unit,
      every: FiniteDuration,
      mat: Materializer
  ): IEventSubscription = {
    subscribe(eventKeys, every).asScala.to(Sink.foreach(callback)).run()(mat)
  }

  def subscribeActorRef(eventKeys: java.util.Set[EventKey], actorRef: ActorRef[Event], mat: Materializer): IEventSubscription = {
    subscribeCallback(eventKeys, event => actorRef ! event, mat)
  }

  def subscribeActorRef(
      eventKeys: java.util.Set[EventKey],
      actorRef: ActorRef[Event],
      every: FiniteDuration,
      mat: Materializer
  ): IEventSubscription = {
    subscribeCallback(eventKeys, event => actorRef ! event, every, mat)
  }

  def get(eventKeys: java.util.Set[EventKey]): CompletableFuture[java.util.Set[Event]]

  def get(eventKey: EventKey): CompletableFuture[Event]
}
