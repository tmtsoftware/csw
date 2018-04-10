package csw.services.event.internal.kafka

import akka.Done
import akka.kafka.scaladsl.Consumer
import akka.kafka.scaladsl.Consumer.Control
import akka.kafka.{ConsumerSettings, ManualSubscription, Subscription, Subscriptions}
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import csw.messages.events._
import csw.services.event.scaladsl.{EventSubscriber, EventSubscription}
import csw_protobuf.events.PbEvent
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class KafkaSubscriber(consumerSettings: ConsumerSettings[String, Array[Byte]])(implicit ec: ExecutionContext,
                                                                               protected val mat: Materializer)
    extends EventSubscriber {

  val consumer: KafkaConsumer[String, Array[Byte]] = consumerSettings.createKafkaConsumer()

  override def subscribe(eventKeys: Set[EventKey]): Source[Event, EventSubscription] = {
    val partitionToOffsets                     = getLatestOffsets(eventKeys)
    val manualSubscription: ManualSubscription = getSubscription(partitionToOffsets)
    val invalidEventStream                     = if (getPublishedCount(partitionToOffsets) == 0) Source.single(Event.invalidEvent) else Source.empty
    val eventStream                            = getEventStream(manualSubscription)

    invalidEventStream
      .concatMat(eventStream)(Keep.right)
      .mapMaterializedValue(eventSubscriptionForControl)
  }

  override def get(eventKeys: Set[EventKey]): Future[Set[Event]] = {
    val partitionToOffsets = getLatestOffsets(eventKeys)
    val manualSubscription = getSubscription(partitionToOffsets)
    val publishedKeyCount  = getPublishedCount(partitionToOffsets)

    if (publishedKeyCount == 0) Future.successful(Set(Event.invalidEvent))
    else {
      val eventStream = getEventStream(manualSubscription)

      val (subscription, eventsF) = eventStream
        .mapMaterializedValue(eventSubscriptionForControl)
        .take(publishedKeyCount)
        .toMat(Sink.seq)(Keep.both)
        .run()

      eventsF.map { events ⇒
        subscription.unsubscribe()
        events.toSet
      }
    }
  }

  override def get(eventKey: EventKey): Future[Event] = get(Set(eventKey)).map(_.head)

  private def eventSubscriptionForControl(control: Control): EventSubscription = () => control.shutdown().map(_ ⇒ Done)

  private def getSubscription(partitionToOffsets: Map[TopicPartition, Long]) = {
    val manualSubscription = Subscriptions.assignmentWithOffset(partitionToOffsets.mapValues(x ⇒ if (x == 0) 0L else x - 1))
    manualSubscription
  }

  private def getEventStream(subscription: Subscription) = {
    Consumer
      .plainSource(consumerSettings, subscription)
      .map(record ⇒ Event.fromPb(PbEvent.parseFrom(record.value())))
  }

  private def getLatestOffsets(eventKeys: Set[EventKey]): Map[TopicPartition, Long] = {
    val topicPartitions = eventKeys.map(e ⇒ new TopicPartition(e.key, 0)).toList
    consumer.endOffsets(topicPartitions.asJava).asScala.toMap.mapValues(_.toLong)
  }

  private def getPublishedCount(offsets: Map[TopicPartition, Long]) = offsets.values.count(_ != 0)

}
