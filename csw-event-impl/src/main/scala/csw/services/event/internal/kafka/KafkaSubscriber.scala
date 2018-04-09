package csw.services.event.internal.kafka

import akka.Done
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
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
    val topicPartitions    = eventKeys.map(e ⇒ new TopicPartition(e.key, 0)).toList
    val partitionToOffsets = getLatestOffsets(topicPartitions, consumer)
    val manualSubscription = Subscriptions.assignmentWithOffset(partitionToOffsets.mapValues(x ⇒ if (x == 0) 0L else x - 1))
    val invalidEventStream = if (isNoEventAvailable(partitionToOffsets)) Source.single(Event.invalidEvent) else Source.empty
    val eventStream = Consumer
      .plainSource(consumerSettings, manualSubscription)
      .map(record ⇒ Event.fromPb(PbEvent.parseFrom(record.value())))

    invalidEventStream
      .concatMat(eventStream)(Keep.right)
      .mapMaterializedValue { control ⇒
        new EventSubscription {
          override def unsubscribe(): Future[Done] = control.shutdown().map(_ ⇒ Done)
        }
      }
  }

  override def get(eventKeys: Set[EventKey]): Future[Set[Event]] = {
    val topicPartitions    = eventKeys.map(e ⇒ new TopicPartition(e.key, 0)).toList
    val partitionToOffsets = getLatestOffsets(topicPartitions, consumer)
    val manualSubscription = Subscriptions.assignmentWithOffset(partitionToOffsets.mapValues(x ⇒ if (x == 0) 0L else x - 1))
    val publishedKeyCount  = partitionToOffsets.values.count(_ != 0)

    if (publishedKeyCount == 0) Future.successful(Set(Event.invalidEvent))
    else {
      val eventStream = Consumer
        .plainSource(consumerSettings, manualSubscription)
        .map(record ⇒ Event.fromPb(PbEvent.parseFrom(record.value())))

      val (subscription, eventsF) = eventStream
        .mapMaterializedValue { control ⇒
          new EventSubscription {
            override def unsubscribe(): Future[Done] = control.shutdown().map(_ ⇒ Done)
          }
        }
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

  private def getLatestOffsets(
      topicPartitions: List[TopicPartition],
      consumer: KafkaConsumer[String, Array[Byte]]
  ): Map[TopicPartition, Long] = {
    consumer.endOffsets(topicPartitions.asJava).asScala.toMap.mapValues(_.toLong)
  }

  private def isNoEventAvailable(offsets: Map[TopicPartition, Long]) = offsets.values.forall(_ == 0)

}
