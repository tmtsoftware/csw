package csw.services.event.internal.redis

import akka.Done
import akka.stream._
import akka.stream.scaladsl.Source
import csw.messages.events.{Event, EventKey}
import csw.services.event.exceptions.PublishFailedException
import csw.services.event.internal.pubsub.{AbstractEventPublisher, JAbstractEventPublisher}
import csw.services.event.javadsl.IEventPublisher
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.{RedisClient, RedisURI}

import scala.async.Async._
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class RedisPublisher(redisURI: RedisURI, redisClient: RedisClient)(implicit ec: ExecutionContext, mat: Materializer)
    extends AbstractEventPublisher {

  private val parallelism = 1

  private lazy val asyncConnectionF: Future[RedisAsyncCommands[EventKey, Event]] =
    redisClient.connectAsync(EventServiceCodec, redisURI).toScala.map(_.async())

  override def publish[Mat](source: Source[Event, Mat], onError: Event ⇒ Unit): Mat =
    publishEvent(source, parallelism, Some(onError))

  override def publish[Mat](source: Source[Event, Mat]): Mat = publishEvent(source, parallelism, None)

  override def publish(event: Event): Future[Done] =
    async {
      val commands = await(asyncConnectionF)
      // allow publish and set to run in parallel
      val publishF = commands.publish(event.eventKey, event).toScala
      await(publishF)
      commands.set(event.eventKey, event)
      Done
    } recover {
      case NonFatal(ex) ⇒ throw PublishFailedException(event)
    }

  override def shutdown(): Future[Done] = asyncConnectionF.flatMap(_.quit().toScala).map(_ ⇒ Done)

  override def asJava: IEventPublisher = new JAbstractEventPublisher(this)
}
