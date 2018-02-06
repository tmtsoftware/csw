package csw.services.event.impl

import csw.services.event.scaladsl.EventServiceDriver
import csw_protobuf.events.PbEvent
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import io.lettuce.core.{ConnectionFuture, RedisClient, RedisURI}

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class RedisDriver(
    redisClient: RedisClient,
    redisURI: RedisURI,
    redisCodec: RedisCodec[String, PbEvent]
)(implicit ec: ExecutionContext)
    extends EventServiceDriver {

  override def publish(channel: String, data: PbEvent): Future[Long] = {

    val pubSubCommands: ConnectionFuture[RedisPubSubReactiveCommands[String, PbEvent]] = redisClient
      .connectPubSubAsync(redisCodec, redisURI)
      .thenApply(t ⇒ t.reactive())

    pubSubCommands.thenCompose[java.lang.Long](command ⇒ command.publish(channel, data).toFuture).toScala.map(_.toLong)
  }

}
