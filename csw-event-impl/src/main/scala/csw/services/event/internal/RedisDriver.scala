package csw.services.event.internal

import csw.services.event.scaladsl.EventServiceDriver
import csw_protobuf.events.PbEvent
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands
import io.lettuce.core.{RedisClient, RedisURI}

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class RedisDriver(
    redisClient: RedisClient,
    redisURI: RedisURI,
    redisCodec: RedisCodec[String, PbEvent]
)(implicit ec: ExecutionContext)
    extends EventServiceDriver {

  private val pubSubCommandsF: Future[StatefulRedisPubSubConnection[String, PbEvent]] =
    redisClient.connectPubSubAsync(redisCodec, redisURI).toScala

  private val commandsF: Future[RedisPubSubAsyncCommands[String, PbEvent]] = pubSubCommandsF.map(_.async())

  override def publish(channel: String, data: PbEvent): Future[Unit] = {

    commandsF
      .flatMap { commands ⇒
        commands.publish(channel, data).toScala.flatMap(_ ⇒ commands.set(channel, data).toScala)
      }
      .map(_ ⇒ ())
  }
}
