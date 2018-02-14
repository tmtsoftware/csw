package csw.services.event.internal

import akka.Done
import akka.stream.scaladsl.{Keep, Source}
import csw.services.event.scaladsl.EventServiceDriver
import csw_protobuf.events.PbEvent
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands
import io.lettuce.core.pubsub.api.reactive.{ChannelMessage, RedisPubSubReactiveCommands}
import io.lettuce.core.{RedisClient, RedisURI}
import reactor.core.publisher.FluxSink.OverflowStrategy

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class ScalaRedisDriver(redisClient: RedisClient, redisURI: RedisURI)(implicit ec: ExecutionContext) extends EventServiceDriver {

  private val pubSubConnectionF: Future[StatefulRedisPubSubConnection[String, PbEvent]] =
    redisClient.connectPubSubAsync(EventServiceCodec, redisURI).toScala

  private val commandsF: Future[RedisPubSubAsyncCommands[String, PbEvent]] = pubSubConnectionF.map(_.async())

  private val pubSubConnectionReactiveF: Future[StatefulRedisPubSubConnection[String, PbEvent]] =
    RedisClient.create(redisURI).connectPubSubAsync(EventServiceCodec, redisURI).toScala

  private val commandsReactiveF: Future[RedisPubSubReactiveCommands[String, PbEvent]] =
    pubSubConnectionReactiveF.map(_.reactive())

  def publish(channel: String, data: PbEvent): Future[Done] = {
    commandsF.flatMap { commands ⇒
      commands
        .publish(channel, data)
        .toScala
        .map(_ ⇒ Done)
    }
  }

  def set(key: String, value: PbEvent): Future[Done] = {
    commandsF.flatMap(_.set(key, value).toScala).map(_ ⇒ Done)
  }

  def subscribe(channels: Seq[String]): Source[ChannelMessage[String, PbEvent], Future[Done]] =
    Source
      .fromFutureSource(commandsReactiveF.map { commands ⇒
        commands.subscribe(channels: _*).subscribe
        Source
          .fromPublisher(commands.observeChannels(OverflowStrategy.LATEST))
          .watchTermination()(Keep.right)
      })
      .mapMaterializedValue(_.flatten)

  def unsubscribe(channels: Seq[String]): Future[Done] = {
    commandsReactiveF.flatMap(_.unsubscribe(channels: _*).toFuture.toScala.map(_ ⇒ Done))
  }

}
