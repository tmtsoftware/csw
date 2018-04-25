package csw.services.event.internal.redis

import acyclic.skipped
import akka.Done
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import csw.messages.events.{Event, EventKey}
import csw.services.event.commons.EventServiceLogger
import csw.services.event.exceptions.PublishFailed
import csw.services.event.javadsl.IEventPublisher
import csw.services.event.scaladsl.EventPublisher
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.{RedisClient, RedisURI}

import scala.async.Async._
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class RedisPublisher(redisURI: RedisURI, redisClient: RedisClient)(implicit ec: ExecutionContext, mat: Materializer)
    extends EventPublisher {
  private val logger = EventServiceLogger.getLogger

  private lazy val asyncConnectionF: Future[RedisAsyncCommands[EventKey, Event]] =
    redisClient.connectAsync(EventServiceCodec, redisURI).toScala.map(_.async())

  override def publish[Mat](source: Source[Event, Mat], onError: (Event, PublishFailed) ⇒ Unit): Mat =
    publishWithOptionalRecovery(source, Some(onError))

  override def publish[Mat](source: Source[Event, Mat]): Mat = publishWithOptionalRecovery(source, None)

  // publish api will fail only if `publish` fails on redis-server and not if `publish` is successful and `set` fails on redis-server
  override def publish(event: Event): Future[Done] =
    async {
      val commands = await(asyncConnectionF)
      await(commands.publish(event.eventKey, event).toScala)
      await(commands.set(event.eventKey, event).toScala.recover { case NonFatal(ex) ⇒ logger.error(ex.getMessage, ex = ex) })
      Done
    } recover {
      case NonFatal(ex) ⇒ throw PublishFailed(event, ex.getMessage)
    }

  override def shutdown(): Future[Done] = asyncConnectionF.flatMap(_.quit().toScala).map(_ ⇒ Done)

  override def asJava: IEventPublisher = new JRedisPublisher(this)

  private def publishWithOptionalRecovery[Mat](
      source: Source[Event, Mat],
      maybeOnError: Option[(Event, PublishFailed) ⇒ Unit]
  ): Mat =
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
