package csw.services.event.impl

import java.lang

import csw.messages.ccs.events.{Event, EventName, SystemEvent}
import csw.messages.params.models.Prefix
import csw.services.event.scaladsl.EventServiceDriver
import csw_protobuf.events.PbEvent
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import io.lettuce.core.{ConnectionFuture, RedisClient, RedisURI}

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future}

class RedisDriver(redisClient: RedisClient, redisURI: RedisURI, redisCodec: RedisCodec[String, PbEvent])
    extends EventServiceDriver {

  override def publishToChannel(channel: String, data: PbEvent): Future[java.lang.Long] = {

    val connection: ConnectionFuture[StatefulRedisPubSubConnection[String, PbEvent]] =
      redisClient.connectPubSubAsync(redisCodec, redisURI)

    val commands: ConnectionFuture[RedisPubSubReactiveCommands[String, PbEvent]] = connection
      .thenApply(t ⇒ t.reactive())

    val published: Future[java.lang.Long] =
      commands.thenCompose[java.lang.Long](command ⇒ command.publish(channel, data).toFuture).toScala

    published
  }

}
