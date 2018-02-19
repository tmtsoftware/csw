package csw.services.event.internal.redis

import akka.Done
import csw.messages.ccs.events.{Event, EventKey}
import csw.services.event.internal.api.EventPublishDriver
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.{RedisClient, RedisURI}

import scala.async.Async._
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class RedisEventPublisherDriver(redisClient: RedisClient, redisURI: RedisURI)(implicit ec: ExecutionContext)
    extends EventPublishDriver {

  private val asyncCommandsF: Future[RedisAsyncCommands[EventKey, Event]] =
    redisClient.connectAsync(EventServiceCodec, redisURI).toScala.map(_.async())

  def publish(eventKey: EventKey, event: Event): Future[Done] = async {
    val commands = await(asyncCommandsF)
    await(commands.publish(eventKey, event).toScala)
    Done
  }

  def set(eventKey: EventKey, event: Event): Future[Done] = async {
    val commands = await(asyncCommandsF)
    await(commands.set(eventKey, event).toScala)
    Done
  }
}
