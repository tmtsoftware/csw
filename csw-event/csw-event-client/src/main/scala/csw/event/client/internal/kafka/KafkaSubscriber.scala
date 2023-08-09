/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.client.internal.kafka

import org.apache.pekko.Done
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.kafka.{ConsumerSettings, Subscription, Subscriptions, scaladsl}
import org.apache.pekko.stream.StreamDetachedException
import org.apache.pekko.stream.scaladsl.{Keep, Sink, Source}
import csw.event.api.scaladsl.{EventSubscriber, EventSubscription, SubscriptionMode}
import csw.event.client.internal.commons.{EventConverter, EventSubscriberUtil}
import csw.event.client.utils.Utils
import csw.params.events.*
import csw.prefix.models.Subsystem
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.TopicPartition

import cps.compat.FutureAsync.*
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

/**
 * An implementation of [[csw.event.api.scaladsl.EventSubscriber]] API which uses Apache Kafka as the provider for publishing
 * and subscribing events.
 *
 * @param consumerSettings  future of settings for pekko-streams-kafka API for Apache Kafka consumer
 * @param actorSystem to be used for performing asynchronous operations
 */
// $COVERAGE-OFF$
private[event] class KafkaSubscriber(consumerSettings: Future[ConsumerSettings[String, Array[Byte]]])(implicit
    actorSystem: ActorSystem[_]
) extends EventSubscriber {

  import actorSystem.executionContext

  private val consumer: Future[Consumer[String, Array[Byte]]] = consumerSettings.map(_.createKafkaConsumer())
  private val eventSubscriberUtil                             = new EventSubscriberUtil()

  override def subscribe(eventKeys: Set[EventKey]): Source[Event, EventSubscription] = {
    val offsetsF = getLatestOffsets(eventKeys)

    // Subscribe to the 0th offset if nothing has been published yet or `current offset - 1` to receive the last published event
    val updatedOffsetsF = offsetsF.map(_.view.mapValues(x => if (x == 0) 0L else x - 1).toMap)

    val manualSubscription = updatedOffsetsF.map(offsets => Subscriptions.assignmentWithOffset(offsets))
    val eventStream        = getEventStream(manualSubscription)

    val invalidEvents = offsetsF.map { partitionToOffset =>
      val events = partitionToOffset.collect {
        case (topicPartition, offset) if offset == 0 => Event.invalidEvent(EventKey(topicPartition.topic()))
      }
      Source(events)
    }

    eventStream
      .prepend(Source.futureSource(invalidEvents))
      .watchTermination()(Keep.both)
      .mapMaterializedValue { case (controlF, completionF) =>
        eventSubscription(controlF, completionF)
      }
  }

  override def subscribe(
      eventKeys: Set[EventKey],
      every: FiniteDuration,
      mode: SubscriptionMode
  ): Source[Event, EventSubscription] =
    subscribe(eventKeys).via(eventSubscriberUtil.subscriptionModeStage(every, mode))

  override def subscribeAsync(eventKeys: Set[EventKey], callback: Event => Future[_]): EventSubscription =
    eventSubscriberUtil.subscribeAsync(subscribe(eventKeys), callback)

  override def subscribeAsync(
      eventKeys: Set[EventKey],
      callback: Event => Future[_],
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription =
    eventSubscriberUtil.subscribeAsync(subscribe(eventKeys, every, mode), callback)

  override def subscribeCallback(eventKeys: Set[EventKey], callback: Event => Unit): EventSubscription =
    eventSubscriberUtil.subscribeCallback(subscribe(eventKeys), callback)

  override def subscribeCallback(
      eventKeys: Set[EventKey],
      callback: Event => Unit,
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription =
    eventSubscriberUtil
      .subscribeCallback(subscribe(eventKeys, every, mode), callback)

  override def subscribeActorRef(eventKeys: Set[EventKey], actorRef: ActorRef[Event]): EventSubscription =
    subscribeCallback(eventKeys, eventSubscriberUtil.actorCallback(actorRef))

  override def subscribeActorRef(
      eventKeys: Set[EventKey],
      actorRef: ActorRef[Event],
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription = subscribeCallback(eventKeys, eventSubscriberUtil.actorCallback(actorRef), every, mode)

  override def pSubscribe(subsystem: Subsystem, pattern: String): Source[Event, EventSubscription] =
    pSubscribe(s"${subsystem.entryName}.*${Utils.globToRegex(pattern)}")

  override def pSubscribeCallback(subsystem: Subsystem, pattern: String, callback: Event => Unit): EventSubscription =
    eventSubscriberUtil.pSubscribe(pSubscribe(subsystem, pattern), callback)

  override def subscribeObserveEvents(): Source[Event, EventSubscription] = pSubscribe(".*.ObserveEvent.*")

  override def get(eventKeys: Set[EventKey]): Future[Set[Event]] = {
    val (subscription, eventsF) = subscribe(eventKeys).take(eventKeys.size).toMat(Sink.seq)(Keep.both).run()

    async {
      await(subscription.ready())
      val events = await(eventsF)
      await(subscription.unsubscribe())
      events.toSet
    }
  }

  override def get(eventKey: EventKey): Future[Event] = get(Set(eventKey)).map(_.head)

  private def pSubscribe(pattern: String) = {
    val subscription = Subscriptions.topicPattern(pattern)
    getEventStream(Future.successful(subscription)).mapMaterializedValue(control =>
      eventSubscription(control, Future.successful(Done))
    )
  }

  private def getEventStream(subscription: Future[Subscription]): Source[Event, Future[scaladsl.Consumer.Control]] = {
    val future = subscription.flatMap(s => consumerSettings.map(c => scaladsl.Consumer.plainSource(c, s)))
    Source.futureSource(future).map(x => EventConverter.toEvent(x.value()))
  }

  // Get the last offset for the given partitions. The last offset of a partition is the offset of the upcoming
  // message, i.e. the offset of the last available message + 1.
  private def getLatestOffsets(eventKeys: Set[EventKey]): Future[Map[TopicPartition, Long]] = {
    val topicPartitions = eventKeys.map(e => new TopicPartition(e.key, 0)).toList
    // any interaction with kafka consumer needs special care to handle multi-threaded access
    consumer.map(consumer =>
      this.synchronized(consumer.endOffsets(topicPartitions.asJava)).asScala.view.mapValues(_.toLong).toMap
    )
  }

  private def eventSubscription(controlF: Future[scaladsl.Consumer.Control], completionF: Future[Done]): EventSubscription = {
    new EventSubscription {
      override def unsubscribe(): Future[Done] =
        controlF.flatMap(_.shutdown()).recover {
          case NonFatal(_: StreamDetachedException) if completionF.isCompleted => Done
        }
      override def ready(): Future[Done] =
        controlF.map(_ => Done).recover {
          case NonFatal(_: StreamDetachedException) if completionF.isCompleted => Done
        }
    }
  }
}
// $COVERAGE-ON$
