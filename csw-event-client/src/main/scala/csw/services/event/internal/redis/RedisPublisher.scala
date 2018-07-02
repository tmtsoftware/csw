package csw.services.event.internal.redis

import akka.Done
import akka.actor.Cancellable
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import csw.messages.events.{Event, EventKey}
import csw.services.event.exceptions.PublishFailure
import csw.services.event.internal.commons.EventPublisherUtil
import csw.services.event.scaladsl.EventPublisher
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.{RedisClient, RedisURI}

import scala.async.Async._
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
 * An implementation of [[csw.services.event.scaladsl.EventPublisher]] API which uses Redis as the provider for publishing
 * and subscribing events.
 * @param redisURI Contains connection details for the Redis/Sentinel connections.
 * @param redisClient A redis client available from lettuce
 * @param ec the execution context to be used for performing asynchronous operations
 * @param mat the materializer to be used for materializing underlying streams
 */
class RedisPublisher(redisURI: RedisURI, redisClient: RedisClient)(implicit ec: ExecutionContext, mat: Materializer)
    extends EventPublisher {

  // inorder to preserve the order of publishing events, the parallelism level is maintained to 1
  private val parallelism        = 1
  private val eventPublisherUtil = new EventPublisherUtil()

  // create underlying connection asynchronously and obtain an instance of `RedisAsyncCommands` to perform
  // redis operations asynchronously
  private lazy val asyncConnectionF: Future[RedisAsyncCommands[EventKey, Event]] = Future.unit
    .flatMap(_ ⇒ redisClient.connectAsync(EventServiceCodec, redisURI).toScala)
    .map(_.async())

  override def publish(event: Event): Future[Done] =
    async {
      val commands = await(asyncConnectionF)
      val publishF = commands.publish(event.eventKey, event).toScala
      await(publishF)
      set(event, commands) // set will run independent of publish
      Done
    } recover {
      case NonFatal(ex) ⇒
        val failure = PublishFailure(event, ex)
        eventPublisherUtil.logError(failure)
        throw failure
    }

  override def publish[Mat](source: Source[Event, Mat]): Mat =
    eventPublisherUtil.publishFromSource(source, parallelism, publish, None)

  override def publish[Mat](source: Source[Event, Mat], onError: PublishFailure ⇒ Unit): Mat =
    eventPublisherUtil.publishFromSource(source, parallelism, publish, Some(onError))

  override def publish(eventGenerator: ⇒ Event, every: FiniteDuration): Cancellable =
    publish(eventPublisherUtil.eventSource(eventGenerator, every))

  override def publish(eventGenerator: ⇒ Event, every: FiniteDuration, onError: PublishFailure ⇒ Unit): Cancellable =
    publish(eventPublisherUtil.eventSource(eventGenerator, every), onError)

  override def shutdown(): Future[Done] = asyncConnectionF.flatMap(_.quit().toScala).map(_ ⇒ Done)

  private def set(event: Event, commands: RedisAsyncCommands[EventKey, Event]): Future[Done] =
    commands.set(event.eventKey, event).toScala.recover { case NonFatal(_) ⇒ Done }.mapTo[Done]
}
