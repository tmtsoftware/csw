package csw.event.client.internal.commons

import akka.Done
import akka.actor.{Cancellable, PoisonPill}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import csw.event.api.exceptions.PublishFailure
import csw.params.events.Event

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

/**
 * Utility class to provided common functionalities to different implementations of EventPublisher
 */
class EventPublisherUtil(implicit ec: ExecutionContext, mat: Materializer) {

  private val logger = EventServiceLogger.getLogger

  lazy val (actorRef, stream) = Source.actorRef[(Event, Promise[Done])](1024, OverflowStrategy.dropHead).preMaterialize()

  def streamTermination(f: Event => Future[Done]): Future[Done] =
    stream
      .mapAsync(1) {
        case (e, p) =>
          f(e).map(p.trySuccess).recover {
            case ex => p.tryFailure(ex)
          }
      }
      .runForeach(_ => ())

  // create an akka stream source out of eventGenerator function
  def getEventSource(
      eventGenerator: => Future[Option[Event]],
      initialDelay: FiniteDuration,
      every: FiniteDuration
  ): Source[Event, Cancellable] =
    Source.tick(initialDelay, every, ()).mapAsync(1)(x => withErrorLogging(eventGenerator))

  def publishFromSource[Mat](
      source: Source[Event, Mat],
      parallelism: Int,
      publish: Event ⇒ Future[Done],
      maybeOnError: Option[PublishFailure ⇒ Unit]
  ): Mat =
    source
      .mapAsync(parallelism) { event ⇒
        publishWithRecovery(event, publish, maybeOnError)
      }
      .to(Sink.ignore)
      .run()

  private def publishWithRecovery(event: Event, publish: Event ⇒ Future[Done], maybeOnError: Option[PublishFailure ⇒ Unit]) =
    publish(event).recover[Done] {
      case failure @ PublishFailure(_, _) ⇒
        maybeOnError.foreach(onError ⇒ onError(failure))
        Done
    }

  def logError(failure: PublishFailure): Unit = logger.error(failure.getMessage, ex = failure)

  def publish(event: Event, isStreamTerminated: Boolean): Future[Done] = {
    val p = Promise[Done]
    if (isStreamTerminated) p.tryFailure(PublishFailure(event, new RuntimeException("Publisher is shutdown")))
    else actorRef ! ((event, p))
    p.future
  }

  def shutdown(): Unit = actorRef ! PoisonPill

  // log error for any exception from provided eventGenerator
  private def withErrorLogging(eventGenerator: => Future[Option[Event]]): Future[Event] =
    eventGenerator
      .collect { case Some(event) => event }
      .recover {
        case NonFatal(ex) ⇒
          logger.error(ex.getMessage, ex = ex)
          throw ex
      }

}
