package csw.services.event.internal.kafka

import akka.Done
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import csw.messages.ccs.events.Event
import csw.services.event.scaladsl.EventPublisher
import org.apache.kafka.clients.producer.{Callback, ProducerRecord}

import scala.concurrent.{ExecutionContext, Future, Promise}

class KafkaPublisher(producerSettings: ProducerSettings[String, Array[Byte]])(implicit ec: ExecutionContext, mat: Materializer)
    extends EventPublisher {

  private val kafkaProducer = producerSettings.createKafkaProducer()
  private val kafkaSink     = Producer.plainSink(producerSettings, kafkaProducer)

  override def publish[Mat](stream: Source[Event, Mat]): Mat = stream.map(convert).to(kafkaSink).run()

  override def publish(event: Event): Future[Done] = {
    val p: Promise[Done] = Promise()
    kafkaProducer.send(convert(event), complete(p))
    p.future
  }

  private def convert(event: Event): ProducerRecord[String, Array[Byte]] =
    new ProducerRecord(event.eventKey.key, Event.typeMapper.toBase(event).toByteArray)

  private def complete(p: Promise[Done]): Callback = {
    case (_, null) ⇒ p.success(Done)
    case (_, ex)   ⇒ p.failure(ex)
  }

  def shutdown(): Future[Unit] = Future { scala.concurrent.blocking(kafkaProducer.close()) }
}
