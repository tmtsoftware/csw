package csw.services.event.internal.kafka

import akka.Done
import akka.kafka.{scaladsl, ConsumerSettings, Subscription, Subscriptions}
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import csw.messages.events._
import csw.services.event.internal.pubsub.BaseEventSubscriber
import csw.services.event.javadsl.IEventSubscriber
import csw.services.event.scaladsl.EventSubscription
import csw_protobuf.events.PbEvent
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.TopicPartition

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class KafkaSubscriber(consumerSettings: ConsumerSettings[String, Array[Byte]])(implicit ec: ExecutionContext,
                                                                               protected val mat: Materializer)
    extends BaseEventSubscriber {

  val consumer: Consumer[String, Array[Byte]] = consumerSettings.createKafkaConsumer()

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

          override def isReady: Future[Done] = Future.successful(Done)
        }
      }
  }

  override def get(eventKeys: Set[EventKey]): Future[Set[Event]] = {
    val (subscription, eventsF) = subscribe(eventKeys).take(eventKeys.size).toMat(Sink.seq)(Keep.both).run()

    eventsF.map { events ⇒
      subscription.unsubscribe()
      events.toSet
    }
  }

  override def get(eventKey: EventKey): Future[Event] = get(Set(eventKey)).map(_.head)

  override def asJava: IEventSubscriber = new JKafkaSubscriber(this)

  private def getEventStream(subscription: Subscription): Source[Event, scaladsl.Consumer.Control] =
    scaladsl.Consumer
      .plainSource(consumerSettings, subscription)
      .map(record ⇒ Event.fromPb(PbEvent.parseFrom(record.value())))

  private def getLatestOffsets(eventKeys: Set[EventKey]): Map[TopicPartition, Long] = {
    val topicPartitions = eventKeys.map(e ⇒ new TopicPartition(e.key, 0)).toList
    consumer.endOffsets(topicPartitions.asJava).asScala.toMap.mapValues(_.toLong)
  }
}
