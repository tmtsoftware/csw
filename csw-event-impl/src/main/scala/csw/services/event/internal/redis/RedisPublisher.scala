package csw.services.event.internal.redis

import akka.Done
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import csw.messages.ccs.events.{Event, EventKey}
import csw.services.event.scaladsl.EventPublisher
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.{RedisClient, RedisURI}

import scala.async.Async._
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class RedisPublisher(redisClient: RedisClient, redisURI: RedisURI)(implicit ec: ExecutionContext, mat: Materializer)
    extends EventPublisher {

  private val asyncCommandsF: Future[RedisAsyncCommands[EventKey, Event]] =
    redisClient.connectAsync(EventServiceCodec, redisURI).toScala.map(_.async())

  override def publish[Mat](source: Source[Event, Mat]): Mat = source.mapAsync(1)(publish).to(Sink.ignore).run()

  override def publish(event: Event): Future[Done] = async {
    val commands = await(asyncCommandsF)
    await(commands.publish(event.eventKey, event).toScala)
    await(commands.set(event.eventKey, event).toScala)
    Done
  }
}
