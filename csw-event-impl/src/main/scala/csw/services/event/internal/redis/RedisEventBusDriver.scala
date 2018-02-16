package csw.services.event.internal.redis

import akka.Done
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.{KillSwitch, KillSwitches}
import csw.services.event.internal.api.{EventBusDriver, EventMessage}
import csw_protobuf.events.PbEvent
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import io.lettuce.core.{RedisClient, RedisURI}
import reactor.core.publisher.FluxSink.OverflowStrategy

import scala.async.Async._
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class RedisEventBusDriver(redisClient: RedisClient, redisURI: RedisURI)(implicit ec: ExecutionContext) extends EventBusDriver {

  private val asyncCommandsF: Future[RedisAsyncCommands[String, PbEvent]] =
    redisClient.connectAsync(EventServiceCodec, redisURI).toScala.map(_.async())

  private val reactiveCommandsF: Future[RedisPubSubReactiveCommands[String, PbEvent]] =
    redisClient.connectPubSubAsync(EventServiceCodec, redisURI).toScala.map(_.reactive())

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

  def subscribe(channels: Seq[String]): Source[EventMessage[String, PbEvent], KillSwitch] = {
    val sourceF = async {
      val commands = await(reactiveCommandsF)
      commands.subscribe(channels: _*).subscribe()
      Source.fromPublisher(commands.observeChannels(OverflowStrategy.LATEST))
    }

    Source
      .fromFutureSource(sourceF)
      .map(EventMessage.from)
      .viaMat(KillSwitches.single)(Keep.right)
  }

  def unsubscribe(channels: Seq[String]): Future[Done] = async {
    val commands = await(reactiveCommandsF)
    await(commands.unsubscribe(channels: _*).toFuture.toScala)
    Done
  }

}
