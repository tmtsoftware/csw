package csw.services.event.internal.kafka

import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.{Source, SourceQueueWithComplete}
import akka.stream.{Materializer, OverflowStrategy}
import akka.{Done, NotUsed}
import csw.messages.ccs.events.Event
import csw.services.event.scaladsl.EventPublisher
import org.apache.kafka.clients.producer.{Callback, ProducerRecord}

import scala.concurrent.{Future, Promise}

class KafkaPublisher(producerSettings: ProducerSettings[String, Array[Byte]])(implicit mat: Materializer) extends EventPublisher {

  private val kafkaProducer = producerSettings.createKafkaProducer()
  private val kafkaSink     = Producer.plainSink(producerSettings, kafkaProducer)

  override def publish(stream: Source[Event, NotUsed]): Future[Done] = stream.map(convert).runWith(kafkaSink)

  override def publish(event: Event): Future[Done] = {
    val p: Promise[Done] = Promise()
    kafkaProducer.send(convert(event), complete(p))
    p.future
  }

  override def queue(bufferSize: Int, overflowStrategy: OverflowStrategy): SourceQueueWithComplete[Event] =
    Source
      .queue(bufferSize, overflowStrategy)
      .map(convert)
      .to(kafkaSink)
      .run()

  private def convert(event: Event): ProducerRecord[String, Array[Byte]] =
    new ProducerRecord(event.eventKey.key, Event.typeMapper.toBase(event).toByteArray)

  private def complete(p: Promise[Done]): Callback = {
    case (_, null) => p.success(Done)
    case (_, ex)   => p.failure(ex)
  }
}
