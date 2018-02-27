package csw.services.event.internal.kafka

import akka.Done
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import csw.messages.ccs.events.{Event, EventKey}
import csw.services.event.scaladsl.{EventSubscriber, EventSubscription}
import csw_protobuf.events.PbEvent
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class KafkaSubscriber(consumerSettings: ConsumerSettings[String, Array[Byte]])(implicit ec: ExecutionContext,
                                                                               protected val mat: Materializer)
    extends EventSubscriber {

  private val consumer: KafkaConsumer[String, Array[Byte]] = consumerSettings.createKafkaConsumer()

  override def subscribe(eventKeys: Set[EventKey]): Source[Event, EventSubscription] = {
    val topicPartitions = eventKeys.map(e => new TopicPartition(e.key, 0)).toList
    val subscription    = Subscriptions.assignmentWithOffset(getLastOffsets(topicPartitions))

    Consumer
      .plainSource(consumerSettings, subscription)
      .map(record => Event.fromPb(PbEvent.parseFrom(record.value())))
      .mapMaterializedValue { control =>
        new EventSubscription {
          override def unsubscribe(): Future[Done] = control.shutdown()
        }
      }
  }

  private def getLastOffsets(topicPartitions: List[TopicPartition]): Map[TopicPartition, Long] = {
    consumer.endOffsets(topicPartitions.asJava).asScala.toMap.mapValues(x => if (x == 0) 0L else x.toLong - 1)
  }

  def shutdown(): Future[Unit] = Future { scala.concurrent.blocking(consumer.close()) }
}
