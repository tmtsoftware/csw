package csw.services.event.internal.redis

import csw.messages.ccs.events.{Event, EventKey}
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import io.lettuce.core.{RedisClient, RedisURI}

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class RedisGateway(redisURI: RedisURI)(implicit ec: ExecutionContext) {
  private lazy val redisClient: RedisClient = RedisClient.create(redisURI)

  def asyncConnectionF(): Future[RedisAsyncCommands[EventKey, Event]] =
    redisClient.connectAsync(EventServiceCodec, redisURI).toScala.map(_.async())

  def reactiveConnectionF(): Future[RedisPubSubReactiveCommands[EventKey, Event]] =
    redisClient.connectPubSubAsync(EventServiceCodec, redisURI).toScala.map(_.reactive())

  def shutdown(): Future[Void] = redisClient.shutdownAsync().toScala
}
