package csw.services.event.internal.kafka

import java.util.concurrent.{Executors, TimeUnit}

import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import csw.messages.ccs.events.Event
import csw.services.event.scaladsl.EventPublisher
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.{ExecutionContext, Future}

class KafkaPublisher(producerSettings: ProducerSettings[String, Array[Byte]])(implicit mat: Materializer) extends EventPublisher {

  private val kafkaProducer = producerSettings.createKafkaProducer()
  private val kafkaSink     = Producer.plainSink(producerSettings, kafkaProducer)
  private val blockingEc    = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(100))

  override def publish(stream: Source[Event, NotUsed]): Future[Done] = stream.map(convert).runWith(kafkaSink)

  override def publish(event: Event): Future[Done] =
    Future {
      kafkaProducer.send(convert(event)).get(10, TimeUnit.SECONDS)
      Done
    }(blockingEc)

  private def convert(event: Event): ProducerRecord[String, Array[Byte]] =
    new ProducerRecord(event.eventKey.key, Event.typeMapper.toBase(event).toByteArray)
}
