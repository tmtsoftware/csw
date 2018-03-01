package csw.services.event.internal.kafka

import akka.Done
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.Materializer
import akka.stream.scaladsl.{Concat, Keep, Sink, Source}
import csw.messages.ccs.events._
import csw.services.event.scaladsl.{EventSubscriber, EventSubscription}
import csw_protobuf.events.PbEvent
import org.apache.kafka.clients.consumer.{ConsumerRecord, KafkaConsumer}
import org.apache.kafka.common.TopicPartition

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class KafkaSubscriber(consumerSettings: ConsumerSettings[String, Array[Byte]])(implicit ec: ExecutionContext,
                                                                               protected val mat: Materializer)
    extends EventSubscriber {

  private val consumer: KafkaConsumer[String, Array[Byte]] = consumerSettings.createKafkaConsumer()

  override def subscribe(eventKeys: Set[EventKey]): Source[Event, EventSubscription] = {
    val topicPartitions    = eventKeys.map(e ⇒ new TopicPartition(e.key, 0)).toList
    val offsets            = getLatestOffsets(topicPartitions)
    val subscription       = Subscriptions.assignmentWithOffset(offsets.mapValues(x ⇒ if (x == 0) 0L else x - 1))
    val invalidEventSource = if (isNoEventAvailable(offsets)) Source.single(invalidRecord()) else Source.empty
    val eventSource        = Consumer.plainSource(consumerSettings, subscription)

    Source
      .combineMat(invalidEventSource, eventSource)(Concat[ConsumerRecord[String, Array[Byte]]])((_, m2) => m2)
      .map(record ⇒ Event.fromPb(PbEvent.parseFrom(record.value())))
      .mapMaterializedValue { control ⇒
        new EventSubscription {
          override def unsubscribe(): Future[Done] = control.shutdown()
        }
      }
  }

  override def get(eventKeys: Set[EventKey]): Future[Set[Event]] = Future.sequence(eventKeys.map(get))

  override def get(eventKey: EventKey): Future[Event] = {
    val (subscription, eventF) = subscribe(Set(eventKey)).toMat(Sink.head)(Keep.both).run()

    eventF.map { event ⇒
      subscription.unsubscribe()
      event
    }
  }

  def shutdown(): Future[Unit] = Future { scala.concurrent.blocking(consumer.close()) }

  private def invalidRecord() = {
    val eventKey = Event.invalidEvent.eventKey.key
    new ConsumerRecord[String, Array[Byte]](eventKey, 0, 0, eventKey, Event.invalidEvent.toPb.toByteArray)
  }

  private def getLatestOffsets(topicPartitions: List[TopicPartition]): Map[TopicPartition, Long] =
    consumer.endOffsets(topicPartitions.asJava).asScala.toMap.mapValues(_.toLong)

  private def isNoEventAvailable(offsets: Map[TopicPartition, Long]) = offsets.values.exists(_ == 0)

}
