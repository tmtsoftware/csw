package csw.services.event.internal.kafka

import akka.Done
import akka.actor.typed.ActorRef
import akka.kafka.{scaladsl, ConsumerSettings, Subscription, Subscriptions}
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import csw.messages.events._
import csw.messages.params.models.Subsystem
import csw.services.event.internal.commons.EventSubscriberUtil
import csw.services.event.scaladsl.{EventSubscriber, EventSubscription, SubscriptionMode}
import csw.services.event.utils.Utils
import csw_protobuf.events.PbEvent
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.TopicPartition

import scala.async.Async.{async, await}
import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
 * An implementation of [[csw.services.event.scaladsl.EventSubscriber]] API which uses Apache Kafka as the provider for publishing
 * and subscribing events.
 * @param consumerSettings Settings for akka-streams-kafka API for Apache Kafka consumer
 * @param ec the execution context to be used for performing asynchronous operations
 * @param mat the materializer to be used for materializing underlying streams
 */
class KafkaSubscriber(consumerSettings: ConsumerSettings[String, Array[Byte]])(
    implicit ec: ExecutionContext,
    mat: Materializer
) extends EventSubscriber {

  private val consumer: Consumer[String, Array[Byte]] = consumerSettings.createKafkaConsumer()
  private val eventSubscriberUtil                     = new EventSubscriberUtil()

  override def subscribe(eventKeys: Set[EventKey]): Source[Event, EventSubscription] = {
    val partitionToOffsets = getLatestOffsets(eventKeys)
    // Subscribe to the 0th offset if nothing has been published yet or `current offset - 1` to receive the last published event
    val manualSubscription = Subscriptions.assignmentWithOffset(partitionToOffsets.mapValues(x ⇒ if (x == 0) 0L else x - 1))
    val eventStream        = getEventStream(manualSubscription)

    val invalidEvents = partitionToOffsets.collect {
      case (topicPartition, offset) if offset == 0 ⇒ Event.invalidEvent(EventKey(topicPartition.topic()))
    }

    Source(invalidEvents)
      .concatMat(eventStream)(Keep.right)
      .mapMaterializedValue(eventSubscription)
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

  override def pSubscribe(subsystem: Subsystem, pattern: String): Source[Event, EventSubscription] = {
    val keyPattern   = s"${subsystem.entryName}.*${Utils.globToRegex(pattern)}"
    val subscription = Subscriptions.topicPattern(keyPattern)
    getEventStream(subscription).mapMaterializedValue(eventSubscription)
  }

  override def pSubscribeCallback(subsystem: Subsystem, pattern: String, callback: Event ⇒ Unit): EventSubscription =
    eventSubscriberUtil.pSubscribe(pSubscribe(subsystem, pattern), callback)

  override def get(eventKeys: Set[EventKey]): Future[Set[Event]] = {
    val (subscription, eventsF) = subscribe(eventKeys).take(eventKeys.size).toMat(Sink.seq)(Keep.both).run()

    async {
      val events = await(eventsF)
      await(subscription.unsubscribe())
      events.toSet
    }
  }

  override def get(eventKey: EventKey): Future[Event] = get(Set(eventKey)).map(_.head)

  private def getEventStream(subscription: Subscription): Source[Event, scaladsl.Consumer.Control] =
    scaladsl.Consumer
      .plainSource(consumerSettings, subscription)
      .map(
        record ⇒
          try Event.fromPb(PbEvent.parseFrom(record.value()))
          catch { case NonFatal(_) ⇒ Event.badEvent() }
      )

  //Get the last offset for the given partitions. The last offset of a partition is the offset of the upcoming
  // message, i.e. the offset of the last available message + 1.
  private def getLatestOffsets(eventKeys: Set[EventKey]): Map[TopicPartition, Long] = {
    val topicPartitions = eventKeys.map(e ⇒ new TopicPartition(e.key, 0)).toList
    consumer.endOffsets(topicPartitions.asJava).asScala.toMap.mapValues(_.toLong)
  }

  private def eventSubscription(control: scaladsl.Consumer.Control): EventSubscription = {
    new EventSubscription {
      override def unsubscribe(): Future[Done] = control.shutdown().map(_ ⇒ Done)

      override def ready(): Future[Done] = Future.successful(Done)
    }
  }
}
