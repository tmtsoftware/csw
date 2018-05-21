package csw.services.event.internal.pubsub

import java.time.Duration
import java.util
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

import akka.Done
import akka.actor.typed.ActorRef
import akka.stream.Materializer
import akka.stream.javadsl.Source
import csw.messages.events.{Event, EventKey}
import csw.services.event.internal.EventServiceExts.RichEventSubscription
import csw.services.event.javadsl.{IEventSubscriber, IEventSubscription}
import csw.services.event.scaladsl.{EventSubscriber, EventSubscription, SubscriptionMode}

import scala.collection.JavaConverters.{asScalaSetConverter, setAsJavaSetConverter}
import scala.compat.java8.DurationConverters.DurationOps
import scala.compat.java8.FutureConverters.{CompletionStageOps, FutureOps}

class JEventSubscriber(eventSubscriber: EventSubscriber) extends IEventSubscriber {

  def subscribe(eventKeys: util.Set[EventKey]): Source[Event, IEventSubscription] =
    eventSubscriber
      .subscribe(eventKeys.asScala.toSet)
      .asJava
      .mapMaterializedValue { eventSubscription: EventSubscription ⇒
        new IEventSubscription {
          override def unsubscribe(): CompletableFuture[Done] = eventSubscription.unsubscribe().toJava.toCompletableFuture

          override def ready(): CompletableFuture[Done] = eventSubscription.ready().toJava.toCompletableFuture
        }
      }

  def subscribe(eventKeys: util.Set[EventKey], every: Duration): Source[Event, IEventSubscription] =
    eventSubscriber
      .subscribe(eventKeys.asScala.toSet, every.toScala, SubscriptionMode.RateAdapterMode)
      .mapMaterializedValue(_.asJava)
      .asJava

  def subscribeAsync(eventKeys: util.Set[EventKey], callback: Event ⇒ CompletableFuture[_]): IEventSubscription =
    eventSubscriber.subscribeAsync(eventKeys.asScala.toSet, e ⇒ callback(e).toScala).asJava

  def subscribeAsync(eventKeys: util.Set[EventKey], callback: Event ⇒ CompletableFuture[_], every: Duration): IEventSubscription =
    eventSubscriber
      .subscribeAsync(eventKeys.asScala.toSet, e ⇒ callback(e).toScala, every.toScala, SubscriptionMode.RateAdapterMode)
      .asJava

  def subscribeCallback(eventKeys: util.Set[EventKey], callback: Consumer[Event]): IEventSubscription =
    eventSubscriber.subscribeCallback(eventKeys.asScala.toSet, e ⇒ callback.accept(e)).asJava

  def subscribeCallback(eventKeys: util.Set[EventKey], callback: Consumer[Event], every: Duration): IEventSubscription =
    eventSubscriber
      .subscribeCallback(eventKeys.asScala.toSet, e ⇒ callback.accept(e), every.toScala, SubscriptionMode.RateAdapterMode)
      .asJava

  def subscribeActorRef(eventKeys: util.Set[EventKey], actorRef: ActorRef[Event], mat: Materializer): IEventSubscription =
    eventSubscriber.subscribeActorRef(eventKeys.asScala.toSet, actorRef).asJava

  def subscribeActorRef(eventKeys: util.Set[EventKey], actorRef: ActorRef[Event], every: Duration): IEventSubscription =
    eventSubscriber.subscribeActorRef(eventKeys.asScala.toSet, actorRef, every.toScala, SubscriptionMode.RateAdapterMode).asJava

  def get(eventKeys: util.Set[EventKey]): CompletableFuture[util.Set[Event]] =
    eventSubscriber.get(eventKeys.asScala.toSet).toJava.toCompletableFuture.thenApply(_.asJava)

  def get(eventKey: EventKey): CompletableFuture[Event] = eventSubscriber.get(eventKey).toJava.toCompletableFuture

  def asScala: EventSubscriber = eventSubscriber
}
