/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.client.internal.commons.javawrappers

import java.time.Duration
import java.util
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.stream.javadsl.Source
import csw.params.events.{Event, EventKey}
import csw.event.api.internal.EventServiceExts.RichEventSubscription
import csw.event.api.javadsl.{IEventSubscriber, IEventSubscription}
import csw.event.api.scaladsl.{EventSubscriber, EventSubscription, SubscriptionMode}
import csw.prefix.models.Subsystem

import scala.jdk.CollectionConverters.*
import scala.jdk.DurationConverters.*
import scala.jdk.FutureConverters.*

/**
 * Java API for [[csw.event.api.scaladsl.EventSubscriber]]
 */
class JEventSubscriber(eventSubscriber: EventSubscriber) extends IEventSubscriber {

  def subscribe(eventKeys: util.Set[EventKey]): Source[Event, IEventSubscription] =
    eventSubscriber
      .subscribe(eventKeys.asScala.toSet)
      .asJava
      .mapMaterializedValue { (eventSubscription: EventSubscription) =>
        new IEventSubscription {
          override def unsubscribe(): CompletableFuture[Done] = eventSubscription.unsubscribe().asJava.toCompletableFuture

          override def ready(): CompletableFuture[Done] = eventSubscription.ready().asJava.toCompletableFuture
        }
      }

  def subscribe(eventKeys: util.Set[EventKey], every: Duration, mode: SubscriptionMode): Source[Event, IEventSubscription] =
    eventSubscriber
      .subscribe(eventKeys.asScala.toSet, every.toScala, mode)
      .mapMaterializedValue(_.asJava)
      .asJava

  def subscribeAsync(eventKeys: util.Set[EventKey], callback: Event => CompletableFuture[_]): IEventSubscription =
    eventSubscriber.subscribeAsync(eventKeys.asScala.toSet, e => callback(e).asScala).asJava

  def subscribeAsync(
      eventKeys: util.Set[EventKey],
      callback: Event => CompletableFuture[_],
      every: Duration,
      mode: SubscriptionMode
  ): IEventSubscription =
    eventSubscriber
      .subscribeAsync(eventKeys.asScala.toSet, e => callback(e).asScala, every.toScala, mode)
      .asJava

  def subscribeCallback(eventKeys: util.Set[EventKey], callback: Consumer[Event]): IEventSubscription =
    eventSubscriber.subscribeCallback(eventKeys.asScala.toSet, e => callback.accept(e)).asJava

  def subscribeCallback(
      eventKeys: util.Set[EventKey],
      callback: Consumer[Event],
      every: Duration,
      mode: SubscriptionMode
  ): IEventSubscription =
    eventSubscriber
      .subscribeCallback(eventKeys.asScala.toSet, e => callback.accept(e), every.toScala, mode)
      .asJava

  def subscribeActorRef(eventKeys: util.Set[EventKey], actorRef: ActorRef[Event]): IEventSubscription =
    eventSubscriber.subscribeActorRef(eventKeys.asScala.toSet, actorRef).asJava

  def subscribeActorRef(
      eventKeys: util.Set[EventKey],
      actorRef: ActorRef[Event],
      every: Duration,
      mode: SubscriptionMode
  ): IEventSubscription =
    eventSubscriber.subscribeActorRef(eventKeys.asScala.toSet, actorRef, every.toScala, mode).asJava

  def pSubscribe(subsystem: Subsystem, pattern: String): Source[Event, IEventSubscription] =
    eventSubscriber.pSubscribe(subsystem, pattern).mapMaterializedValue(_.asJava).asJava

  def pSubscribeCallback(subsystem: Subsystem, pattern: String, callback: Consumer[Event]): IEventSubscription =
    eventSubscriber.pSubscribeCallback(subsystem, pattern, e => callback.accept(e)).asJava

  def subscribeObserveEvents(): Source[Event, IEventSubscription] =
    eventSubscriber.subscribeObserveEvents().mapMaterializedValue(_.asJava).asJava

  def get(eventKeys: util.Set[EventKey]): CompletableFuture[util.Set[Event]] =
    eventSubscriber.get(eventKeys.asScala.toSet).asJava.toCompletableFuture.thenApply(_.asJava)

  def get(eventKey: EventKey): CompletableFuture[Event] = eventSubscriber.get(eventKey).asJava.toCompletableFuture

  def asScala: EventSubscriber = eventSubscriber
}
