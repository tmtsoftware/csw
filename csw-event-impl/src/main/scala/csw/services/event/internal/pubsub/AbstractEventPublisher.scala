package csw.services.event.internal.pubsub

import akka.actor.Cancellable
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import csw.messages.events.Event
import csw.services.event.exceptions.PublishFailedException
import csw.services.event.internal.commons.EventServiceLogger
import csw.services.event.scaladsl.EventPublisher

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.util.control.NonFatal

abstract class AbstractEventPublisher(implicit ec: ExecutionContext, mat: Materializer) extends EventPublisher {

  private val logger = EventServiceLogger.getLogger

  override def publish(eventGenerator: => Event, every: FiniteDuration): Cancellable = publish(eventStream(eventGenerator, every))

  override def publish(eventGenerator: ⇒ Event, every: FiniteDuration, onError: Event ⇒ Unit): Cancellable =
    publish(eventStream(eventGenerator, every), onError)

  private def eventStream(eventGenerator: => Event, every: FiniteDuration): Source[Event, Cancellable] =
    Source.tick(0.millis, every, ()).map(_ => eventGenerator)

  def publishEvent[Mat](source: Source[Event, Mat], parallelism: Int, maybeOnError: Option[Event ⇒ Unit]): Mat =
    source
      .mapAsync(parallelism) { publishWithRecovery(maybeOnError) }
      .mapError {
        case NonFatal(ex) ⇒
          logger.error(ex.getMessage, ex = ex)
          ex
      }
      .to(Sink.ignore)
      .run()

  private def publishWithRecovery(maybeOnError: Option[Event ⇒ Unit]) = { e: Event ⇒
    publish(e).recover {
      case _ @PublishFailedException(event) ⇒ maybeOnError.foreach(onError ⇒ onError(event))
    }
  }
}
