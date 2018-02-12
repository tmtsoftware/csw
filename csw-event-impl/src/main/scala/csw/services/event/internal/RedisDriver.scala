package csw.services.event.internal

import akka.stream.scaladsl.{Sink, Source, SourceQueueWithComplete}
import akka.stream.{Materializer, OverflowStrategy}
import csw.services.event.scaladsl.EventServiceDriver
import csw_protobuf.events.PbEvent
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands
import io.lettuce.core.{RedisClient, RedisURI}

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future, Promise}

class RedisDriver(redisClient: RedisClient, redisURI: RedisURI)(implicit ec: ExecutionContext, mat: Materializer)
    extends EventServiceDriver {

  private val pubSubCommandsF: Future[StatefulRedisPubSubConnection[String, PbEvent]] =
    redisClient.connectPubSubAsync(EventServiceCodec, redisURI).toScala

  private val commandsF: Future[RedisPubSubAsyncCommands[String, PbEvent]] = pubSubCommandsF.map(_.async())

  private val eventQueue: SourceQueueWithComplete[(String, PbEvent, Promise[String])] = Source
    .queue[(String, PbEvent, Promise[String])](1, OverflowStrategy.dropHead)
    .mapAsync(1) {
      case (channel, data, p) ⇒
        commandsF.flatMap { commands ⇒
          commands
            .publish(channel, data)
            .toScala
            .flatMap(_ ⇒ commands.set(channel, data).toScala)
            .transformWith(t ⇒ p.complete(t).future)
        }
    }
    .to(Sink.ignore)
    .run()

  override def publish(channel: String, data: PbEvent): Future[Unit] = {
    val p = Promise[String]
    eventQueue.offer((channel, data, p))
    p.future.map(_ ⇒ ())
  }
}
