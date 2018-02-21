package csw.services.event.internal.kafka

import akka.Done
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.typed.ActorRef
import csw.messages.ccs.events.{Event, EventKey}
import csw.services.event.scaladsl.{EventSubscriber, EventSubscription}
import csw_protobuf.events.PbEvent
import org.apache.kafka.common.TopicPartition

import scala.concurrent.Future

class KafkaSubscriber(consumerSettings: ConsumerSettings[String, Array[Byte]])(implicit mat: Materializer)
    extends EventSubscriber {

  override def subscribe(eventKeys: Set[EventKey]): Source[Event, EventSubscription] = {
    val subscription = Subscriptions.assignment(eventKeys.map(x => new TopicPartition(x.key, 0)))
    Consumer
      .plainSource(consumerSettings, subscription)
      .map(record => Event.fromPb(PbEvent.parseFrom(record.value())))
      .mapMaterializedValue { control =>
        new EventSubscription {
          override def unsubscribe(): Future[Done] = control.shutdown()
        }
      }
  }

  override def subscribe(eventKeys: Set[EventKey], callback: Event => Unit): EventSubscription =
    subscribe(eventKeys).to(Sink.foreach(callback)).run()

  override def subscribe(eventKeys: Set[EventKey], actorRef: ActorRef[Event]): EventSubscription =
    subscribe(eventKeys, event => actorRef ! event)

}
