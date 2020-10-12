package csw.event.client.internal.commons

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.{Cancellable, PoisonPill}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Sink, Source}
import csw.event.api.exceptions.PublishFailure
import csw.params.events.Event

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, Promise}
import scala.util.control.NonFatal

/**
 * Utility class to provided common functionalities to different implementations of EventPublisher
 */
private[event] class EventPublisherUtil(implicit actorSystem: ActorSystem[_]) {

  private val logger = EventServiceLogger.getLogger

  import EventStreamSupervisionStrategy.attributes
  import actorSystem.executionContext

  lazy val (actorRef, stream) = Source
    .actorRef[(Event, Promise[Done])](
      PartialFunction.empty,
      PartialFunction.empty,
      1024,
      OverflowStrategy.dropHead
    )
    .preMaterialize()

  def streamTermination(f: Event => Future[Done]): Future[Done] =
    stream
      .mapAsync(1) {
        case (e, p) =>
          f(e).map(p.trySuccess).recover {
            case ex => p.tryFailure(ex)
          }
      }
      .runForeach(_ => ())

  private def tick(initialDelay: FiniteDuration, every: FiniteDuration): Source[Unit, Cancellable] = {
    // buffer size of the queue should be 0 so as to follow the semantics of Source.tick
    Source.queue[Unit](0, OverflowStrategy.dropHead).mapMaterializedValue { q =>
      val cancellable = actorSystem.scheduler.scheduleAtFixedRate(initialDelay, every)(() => q.offer(()))
      q.watchCompletion().onComplete(_ => cancellable.cancel())
      cancellable
    }
  }

  // create an akka stream source out of eventGenerator function
  def eventSource(
      eventGenerator: => Future[Option[Event]],
      parallelism: Int,
      initialDelay: FiniteDuration,
      every: FiniteDuration
  ): Source[Event, Cancellable] = {
    tick(initialDelay, every).mapAsync(parallelism)(_ => withErrorLogging(eventGenerator))
  }

  def publishFromSource[Mat](
      source: Source[Event, Mat],
      parallelism: Int,
      publish: Event => Future[Done],
      maybeOnError: Option[PublishFailure => Unit]
  ): Mat =
    source
      .mapAsync(parallelism) { event => publishWithRecovery(event, publish, maybeOnError) }
      .withAttributes(attributes)
      .to(Sink.ignore)
      .run()

  private def publishWithRecovery(event: Event, publish: Event => Future[Done], maybeOnError: Option[PublishFailure => Unit]) =
    publish(event).recover[Done] {
      case failure @ PublishFailure(_, _) =>
        maybeOnError.foreach(onError => onError(failure))
        Done
    }

  def logError(failure: PublishFailure): Unit = logger.error(failure.getMessage, ex = failure)

  def publish(event: Event, isStreamTerminated: Boolean): Future[Done] = {
    val p = Promise[Done]()
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
        case NonFatal(ex) =>
          logger.error(ex.getMessage, ex = ex)
          throw ex
      }

}
