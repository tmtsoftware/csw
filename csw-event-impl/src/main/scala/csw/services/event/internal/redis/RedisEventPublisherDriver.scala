package csw.services.event.internal.redis

import akka.Done
import csw.services.event.internal.api.EventPublishDriver
import csw_protobuf.events.PbEvent
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.{RedisClient, RedisURI}

import scala.async.Async._
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class RedisEventPublisherDriver(redisClient: RedisClient, redisURI: RedisURI)(implicit ec: ExecutionContext)
    extends EventPublishDriver {

  private val asyncCommandsF: Future[RedisAsyncCommands[String, PbEvent]] =
    redisClient.connectAsync(EventServiceCodec, redisURI).toScala.map(_.async())

  def publish(channel: String, data: PbEvent): Future[Done] = async {
    val commands = await(asyncCommandsF)
    await(commands.publish(channel, data).toScala)
    Done
  }

  def set(key: String, value: PbEvent): Future[Done] = async {
    val commands = await(asyncCommandsF)
    await(commands.set(key, value).toScala)
    Done
  }
}
