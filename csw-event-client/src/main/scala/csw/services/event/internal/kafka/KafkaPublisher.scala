package csw.services.event.internal.kafka

import akka.Done
import akka.actor.Cancellable
import akka.kafka.ProducerSettings
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import csw.messages.events.Event
import csw.messages.params.pb.TypeMapperSupport
import csw.services.event.api.exceptions.PublishFailure
import csw.services.event.api.scaladsl.EventPublisher
import csw.services.event.internal.commons.EventPublisherUtil
import org.apache.kafka.clients.producer.{Callback, ProducerRecord}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

/**
 * An implementation of [[csw.services.event.api.scaladsl.EventPublisher]] API which uses Apache Kafka as the provider for publishing
 * and subscribing events.
 * @param producerSettings future of settings for akka-streams-kafka API for Apache Kafka producer
 * @param ec               the execution context to be used for performing asynchronous operations
 * @param mat              the materializer to be used for materializing underlying streams
 */
class KafkaPublisher(producerSettings: Future[ProducerSettings[String, Array[Byte]]])(
    implicit ec: ExecutionContext,
    mat: Materializer
) extends EventPublisher {

  private val parallelism        = 1
  private val kafkaProducer      = producerSettings.map(_.createKafkaProducer())
  private val eventPublisherUtil = new EventPublisherUtil()

  override def publish(event: Event): Future[Done] = {
    val promisedDone: Promise[Done] = Promise()
    kafkaProducer.map(_.send(eventToProducerRecord(event), completePromise(event, promisedDone))).recover {
      case NonFatal(ex) ⇒ promisedDone.failure(PublishFailure(event, ex))
    }
    promisedDone.future
  }

  override def publish[Mat](source: Source[Event, Mat]): Mat =
    eventPublisherUtil.publishFromSource(source, parallelism, publish, None)

  override def publish[Mat](stream: Source[Event, Mat], onError: PublishFailure ⇒ Unit): Mat =
    eventPublisherUtil.publishFromSource(stream, parallelism, publish, Some(onError))

  override def publish(eventGenerator: ⇒ Event, every: FiniteDuration): Cancellable =
    publish(eventPublisherUtil.eventSource(eventGenerator, every))

  override def publish(eventGenerator: ⇒ Event, every: FiniteDuration, onError: PublishFailure ⇒ Unit): Cancellable =
    publish(eventPublisherUtil.eventSource(eventGenerator, every), onError)

  override def shutdown(): Future[Done] = kafkaProducer.map { x =>
    scala.concurrent.blocking(x.close())
    Done
  }

  private def eventToProducerRecord(event: Event): ProducerRecord[String, Array[Byte]] =
    new ProducerRecord(event.eventKey.key, TypeMapperSupport.eventTypeMapper.toBase(event).toByteArray)

  // callback to be complete the future operation for publishing when the record has been acknowledged by the server
  private def completePromise(event: Event, promisedDone: Promise[Done]): Callback = {
    case (_, null)          ⇒ promisedDone.success(Done)
    case (_, ex: Exception) ⇒ promisedDone.failure(PublishFailure(event, ex))
  }
}
