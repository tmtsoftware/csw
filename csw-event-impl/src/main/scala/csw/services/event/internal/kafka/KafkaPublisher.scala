package csw.services.event.internal.kafka

import akka.Done
import akka.kafka.ProducerSettings
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import csw.messages.events.Event
import csw.services.event.commons.EventServiceLogger
import csw.services.event.exceptions.PublishFailed
import csw.services.event.scaladsl.EventPublisher
import org.apache.kafka.clients.producer.{Callback, ProducerRecord}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

class KafkaPublisher(producerSettings: ProducerSettings[String, Array[Byte]])(implicit ec: ExecutionContext, mat: Materializer)
    extends EventPublisher {
  private val logger = EventServiceLogger.getLogger

  private val kafkaProducer = producerSettings.createKafkaProducer()

  override def publish[Mat](stream: Source[Event, Mat], onError: (Event, Throwable) ⇒ Unit): Mat =
    publishWithOptionalRecovery(stream, Some(onError))

  override def publish(event: Event): Future[Done] = {
    val p: Promise[Done] = Promise()
    try {
      kafkaProducer.send(convert(event), complete(event, p))
    } catch {
      case NonFatal(ex) ⇒ p.failure(PublishFailed(event, ex.getMessage))
    }
    p.future
  }

  override def shutdown(): Future[Done] = Future {
    scala.concurrent.blocking(kafkaProducer.close())
    Done
  }

  private def convert(event: Event): ProducerRecord[String, Array[Byte]] =
    new ProducerRecord(event.eventKey.key, Event.typeMapper.toBase(event).toByteArray)

  private def complete(event: Event, p: Promise[Done]): Callback = {
    case (_, null)          ⇒ p.success(Done)
    case (_, ex: Exception) ⇒ p.failure(PublishFailed(event, ex.getMessage))
  }

  override def publish[Mat](source: Source[Event, Mat]): Mat = publishWithOptionalRecovery(source, None)

  private def publishWithOptionalRecovery[Mat](source: Source[Event, Mat], maybeOnError: Option[(Event, Throwable) ⇒ Unit]): Mat =
    source
      .mapAsync(1) {
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
