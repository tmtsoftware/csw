package csw.services.event.internal.kafka

import akka.Done
import akka.kafka.ProducerSettings
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import csw.messages.events.Event
import csw.services.event.internal.commons.EventServiceLogger
import csw.services.event.exceptions.PublishFailed
import csw.services.event.internal.pubsub.{AbstractEventPublisher, JAbstractEventPublisher}
import csw.services.event.javadsl.IEventPublisher
import org.apache.kafka.clients.producer.{Callback, ProducerRecord}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

class KafkaPublisher(producerSettings: ProducerSettings[String, Array[Byte]])(implicit ec: ExecutionContext, mat: Materializer)
    extends AbstractEventPublisher {

  private val logger = EventServiceLogger.getLogger

  private val kafkaProducer = producerSettings.createKafkaProducer()

  override def publish[Mat](stream: Source[Event, Mat], onError: (Event, PublishFailed) ⇒ Unit): Mat =
    publishWithRecovery(stream, Some(onError))

  override def publish(event: Event): Future[Done] = {
    val promisedDone: Promise[Done] = Promise()
    try {
      kafkaProducer.send(eventToProducerRecord(event), completePromise(event, promisedDone))
    } catch {
      case NonFatal(ex) ⇒ promisedDone.failure(PublishFailed(event, ex.getMessage))
    }
    promisedDone.future
  }

  override def shutdown(): Future[Done] = Future {
    scala.concurrent.blocking(kafkaProducer.close())
    Done
  }

  private def eventToProducerRecord(event: Event): ProducerRecord[String, Array[Byte]] =
    new ProducerRecord(event.eventKey.key, Event.typeMapper.toBase(event).toByteArray)

  private def completePromise(event: Event, promisedDone: Promise[Done]): Callback = {
    case (_, null)          ⇒ promisedDone.success(Done)
    case (_, ex: Exception) ⇒ promisedDone.failure(PublishFailed(event, ex.getMessage))
  }

  override def publish[Mat](source: Source[Event, Mat]): Mat = publishWithRecovery(source, None)

  override def asJava: IEventPublisher = new JAbstractEventPublisher(this)

  private def publishWithRecovery[Mat](source: Source[Event, Mat], maybeOnError: Option[(Event, PublishFailed) ⇒ Unit]): Mat =
    source
      .mapAsync(100) {
        maybeOnError match {
          case Some(onError) ⇒ publish(_).recover { case ex @ PublishFailed(event, _) ⇒ onError(event, ex) }
          case None          ⇒ publish
        }
      }
      .mapError {
        case NonFatal(ex) ⇒
          logger.error(ex.getMessage, ex = ex)
          ex
      }
      .to(Sink.ignore)
      .run()
}
