package csw.event.client.internal.kafka

import akka.Done
import akka.actor.Cancellable
import akka.kafka.ProducerSettings
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import csw.event.api.exceptions.PublishFailure
import csw.event.api.scaladsl.EventPublisher
import csw.event.client.internal.commons.EventPublisherUtil
import csw.event.client.pb.TypeMapperSupport
import csw.params.events.Event
import csw.time.core.models.TMTTime
import org.apache.kafka.clients.producer.{Callback, ProducerRecord}

import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

/**
 * An implementation of [[csw.event.api.scaladsl.EventPublisher]] API which uses Apache Kafka as the provider for publishing
 * and subscribing events.
 *
 * @param producerSettings future of settings for akka-streams-kafka API for Apache Kafka producer
 * @param ec               the execution context to be used for performing asynchronous operations
 * @param mat              the materializer to be used for materializing underlying streams
 */
class KafkaPublisher(producerSettings: Future[ProducerSettings[String, Array[Byte]]])(implicit ec: ExecutionContext,
                                                                                      mat: Materializer)
    extends EventPublisher {

  private val parallelism                         = 1
  private val defaultInitialDelay: FiniteDuration = 0.millis
  private val kafkaProducer                       = producerSettings.map(_.createKafkaProducer())
  private val eventPublisherUtil                  = new EventPublisherUtil()

  private val streamTermination: Future[Done] = eventPublisherUtil.streamTermination(publishInternal)

  override def publish(event: Event): Future[Done] = {
    eventPublisherUtil.publish(event, streamTermination.isCompleted)
  }

  private def publishInternal(event: Event): Future[Done] = {
    val p = Promise[Done]
    kafkaProducer.map(_.send(eventToProducerRecord(event), completePromise(event, p))).recover {
      case NonFatal(ex) ⇒ p.failure(PublishFailure(event, ex))
    }
    p.future
  }

  override def publish[Mat](source: Source[Event, Mat]): Mat =
    eventPublisherUtil.publishFromSource(source, parallelism, publishInternal, None)

  override def publish[Mat](source: Source[Event, Mat], onError: PublishFailure ⇒ Unit): Mat =
    eventPublisherUtil.publishFromSource(source, parallelism, publishInternal, Some(onError))

  override def publish(eventGenerator: ⇒ Option[Event], every: FiniteDuration): Cancellable =
    publish(eventPublisherUtil.eventSource(Future.successful(eventGenerator), parallelism, defaultInitialDelay, every))

  override def publish(eventGenerator: => Option[Event], startTime: TMTTime, every: FiniteDuration): Cancellable =
    publish(eventPublisherUtil.eventSource(Future.successful(eventGenerator), parallelism, startTime.durationFromNow, every))

  override def publish(eventGenerator: ⇒ Option[Event], every: FiniteDuration, onError: PublishFailure ⇒ Unit): Cancellable =
    publish(eventPublisherUtil.eventSource(Future.successful(eventGenerator), parallelism, defaultInitialDelay, every), onError)

  override def publish(
      eventGenerator: => Option[Event],
      startTime: TMTTime,
      every: FiniteDuration,
      onError: PublishFailure => Unit
  ): Cancellable =
    publish(eventPublisherUtil.eventSource(Future.successful(eventGenerator), parallelism, startTime.durationFromNow, every),
            onError)

  override def publishAsync(eventGenerator: => Future[Option[Event]], every: FiniteDuration): Cancellable =
    publish(eventPublisherUtil.eventSource(eventGenerator, parallelism, defaultInitialDelay, every))

  override def publishAsync(eventGenerator: => Future[Option[Event]], startTime: TMTTime, every: FiniteDuration): Cancellable =
    publish(eventPublisherUtil.eventSource(eventGenerator, parallelism, startTime.durationFromNow, every))

  override def publishAsync(
      eventGenerator: => Future[Option[Event]],
      every: FiniteDuration,
      onError: PublishFailure => Unit
  ): Cancellable =
    publish(eventPublisherUtil.eventSource(eventGenerator, parallelism, defaultInitialDelay, every), onError)

  override def publishAsync(
      eventGenerator: => Future[Option[Event]],
      startTime: TMTTime,
      every: FiniteDuration,
      onError: PublishFailure => Unit
  ): Cancellable =
    publish(eventPublisherUtil.eventSource(eventGenerator, parallelism, startTime.durationFromNow, every), onError)

  override def shutdown(): Future[Done] = kafkaProducer.map { x =>
    eventPublisherUtil.shutdown()
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
