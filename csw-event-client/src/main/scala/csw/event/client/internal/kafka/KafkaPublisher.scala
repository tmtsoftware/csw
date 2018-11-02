package csw.event.client.internal.kafka

import akka.Done
import akka.actor.Cancellable
import akka.kafka.ProducerSettings
import akka.stream.scaladsl.Source
import akka.stream.{Materializer, OverflowStrategy}
import csw.event.api.exceptions.PublishFailure
import csw.event.api.scaladsl.EventPublisher
import csw.event.client.internal.commons.EventPublisherUtil
import csw.event.client.pb.TypeMapperSupport
import csw.params.events.Event
import org.apache.kafka.clients.producer.{Callback, ProducerRecord}

import scala.concurrent.duration.FiniteDuration
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

  private val parallelism        = 1
  private val kafkaProducer      = producerSettings.map(_.createKafkaProducer())
  private val eventPublisherUtil = new EventPublisherUtil()
  private val (actorRef, stream) = Source.actorRef[(Event, Promise[Done])](1024, OverflowStrategy.dropHead).preMaterialize()

  stream
    .mapAsync(1) {
      case (e, p) =>
        publishInternal(e).map(p.trySuccess).recover {
          case ex => p.tryFailure(ex)
        }
    }
    .runForeach(_ => ())

  override def publish(event: Event): Future[Done] = {
    val p = Promise[Done]
    actorRef ! ((event, p))
    p.future
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

  override def publish[Mat](stream: Source[Event, Mat], onError: PublishFailure ⇒ Unit): Mat =
    eventPublisherUtil.publishFromSource(stream, parallelism, publishInternal, Some(onError))

  override def publish(eventGenerator: ⇒ Event, every: FiniteDuration): Cancellable =
    publish(eventPublisherUtil.eventSource(eventGenerator, every))

  override def publish(eventGenerator: ⇒ Event, every: FiniteDuration, onError: PublishFailure ⇒ Unit): Cancellable =
    publish(eventPublisherUtil.eventSource(eventGenerator, every), onError)

  override def publishAsync(eventGenerator: => Future[Event], every: FiniteDuration): Cancellable =
    publish(eventPublisherUtil.eventSourceAsync(eventGenerator, every))

  override def publishAsync(eventGenerator: => Future[Event],
                            every: FiniteDuration,
                            onError: PublishFailure => Unit): Cancellable =
    publish(eventPublisherUtil.eventSourceAsync(eventGenerator, every), onError)

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
