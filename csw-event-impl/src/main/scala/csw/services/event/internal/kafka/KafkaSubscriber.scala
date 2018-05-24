package csw.services.event.internal.kafka

import akka.Done
import akka.actor.typed.ActorRef
import akka.kafka.{scaladsl, ConsumerSettings, Subscription, Subscriptions}
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import csw.messages.events._
import csw.services.event.internal.pubsub.{EventSubscriberUtil, JEventSubscriber}
import csw.services.event.javadsl.IEventSubscriber
import csw.services.event.scaladsl.{EventSubscriber, EventSubscription, SubscriptionMode}
import csw_protobuf.events.PbEvent
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.TopicPartition

import scala.async.Async.{async, await}
import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class KafkaSubscriber(
    consumerSettings: ConsumerSettings[String, Array[Byte]],
    eventSubscriberUtil: EventSubscriberUtil
)(implicit ec: ExecutionContext, mat: Materializer)
    extends EventSubscriber {

  private val consumer: Consumer[String, Array[Byte]] = consumerSettings.createKafkaConsumer()

  override def subscribe(eventKeys: Set[EventKey]): Source[Event, EventSubscription] = {
    val partitionToOffsets = getLatestOffsets(eventKeys)
    val manualSubscription = Subscriptions.assignmentWithOffset(partitionToOffsets.mapValues(x ⇒ if (x == 0) 0L else x - 1))
    val eventStream        = getEventStream(manualSubscription)

    val invalidEvents = partitionToOffsets.collect {
      case (topicPartition, offset) if offset == 0 ⇒ Event.invalidEvent(EventKey(topicPartition.topic()))
    }

    Source(invalidEvents)
      .concatMat(eventStream)(Keep.right)
      .mapMaterializedValue { control ⇒
        new EventSubscription {
          override def unsubscribe(): Future[Done] = control.shutdown().map(_ ⇒ Done)

          override def ready(): Future[Done] = Future.successful(Done)
        }
      }
  }

  override def get(eventKeys: Set[EventKey]): Future[Set[Event]] = {
    val (subscription, eventsF) = subscribe(eventKeys).take(eventKeys.size).toMat(Sink.seq)(Keep.both).run()

    async {
      val events = await(eventsF)
      await(subscription.unsubscribe())
      events.toSet
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
    eventSubscriberUtil
      .subscribeAsync(subscribe(eventKeys).via(eventSubscriberUtil.subscriptionModeStage(every, mode)), callback)

  override def subscribeCallback(eventKeys: Set[EventKey], callback: Event => Unit): EventSubscription =
    eventSubscriberUtil
      .subscribeCallback(subscribe(eventKeys), callback)

  override def subscribeCallback(
      eventKeys: Set[EventKey],
      callback: Event => Unit,
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription =
    eventSubscriberUtil
      .subscribeCallback(subscribe(eventKeys).via(eventSubscriberUtil.subscriptionModeStage(every, mode)), callback)

  override def subscribeActorRef(eventKeys: Set[EventKey], actorRef: ActorRef[Event]): EventSubscription =
    subscribeCallback(eventKeys, eventSubscriberUtil.actorCallback(actorRef))

  override def subscribeActorRef(
      eventKeys: Set[EventKey],
      actorRef: ActorRef[Event],
      every: FiniteDuration,
      mode: SubscriptionMode
  ): EventSubscription = subscribeCallback(eventKeys, eventSubscriberUtil.actorCallback(actorRef), every, mode)

  override def pSubscribe(pattern: String): Source[Event, EventSubscription] = ???

  override def get(eventKey: EventKey): Future[Event] = get(Set(eventKey)).map(_.head)

  override def asJava: IEventSubscriber = new JEventSubscriber(this)

  private def getEventStream(subscription: Subscription): Source[Event, scaladsl.Consumer.Control] =
    scaladsl.Consumer
      .plainSource(consumerSettings, subscription)
      .map(record ⇒ Event.fromPb(PbEvent.parseFrom(record.value())))

  private def getLatestOffsets(eventKeys: Set[EventKey]): Map[TopicPartition, Long] = {
    val topicPartitions = eventKeys.map(e ⇒ new TopicPartition(e.key, 0)).toList
    consumer.endOffsets(topicPartitions.asJava).asScala.toMap.mapValues(_.toLong)
  }
}
