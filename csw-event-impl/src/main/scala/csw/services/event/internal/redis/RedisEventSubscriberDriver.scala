package csw.services.event.internal.redis

import akka.Done
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.{KillSwitch, KillSwitches}
import csw.messages.ccs.events.{Event, EventKey}
import csw.services.event.internal.api.{EventMessage, EventSubscriberDriver}
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import io.lettuce.core.{RedisClient, RedisURI}
import reactor.core.publisher.FluxSink.OverflowStrategy

import scala.async.Async._
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class RedisEventSubscriberDriver(redisClient: RedisClient, redisURI: RedisURI)(implicit ec: ExecutionContext)
    extends EventSubscriberDriver {

  private val reactiveCommandsF: Future[RedisPubSubReactiveCommands[EventKey, Event]] =
    redisClient.connectPubSubAsync(EventServiceCodec, redisURI).toScala.map(_.reactive())

  def subscribe(channels: Seq[EventKey]): Source[EventMessage[EventKey, Event], KillSwitch] = {
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

  def unsubscribe(channels: Seq[EventKey]): Future[Done] = async {
    val commands = await(reactiveCommandsF)
    await(commands.unsubscribe(channels: _*).toFuture.toScala)
    Done
  }

}
