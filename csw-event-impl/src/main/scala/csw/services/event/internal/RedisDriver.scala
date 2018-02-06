package csw.services.event.internal

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

  override def publish(channel: String, data: PbEvent): Future[Unit] = {

    val pubSubCommands: ConnectionFuture[RedisPubSubReactiveCommands[String, PbEvent]] = redisClient
      .connectPubSubAsync(redisCodec, redisURI)
      .thenApply(t ⇒ t.reactive())

    pubSubCommands
      .thenAccept(command ⇒ {
        command.multi.subscribe(x ⇒ {
          command.publish(channel, data).subscribe()
          command.set(channel, data).subscribe()
          command.exec().subscribe()
        })
      })
      .toScala
      .map(_ ⇒ ())

  }
}
