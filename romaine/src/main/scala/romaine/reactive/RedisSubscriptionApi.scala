package romaine.reactive

import akka.NotUsed
import akka.stream.scaladsl.{Keep, Source}
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import reactor.core.publisher.FluxSink.OverflowStrategy
import romaine.extensions.SourceExtensions.RichSource

import scala.concurrent.ExecutionContext

class RedisSubscriptionApi[K, V](reactiveApiFactory: () => RedisPubSubReactiveCommands[K, V])(implicit ec: ExecutionContext) {
  def subscribe(keys: List[K], overflowStrategy: OverflowStrategy): Source[RedisResult[K, V], RedisSubscription] =
    subscribeInternal(keys, overflowStrategy, new RedisSubscribeScalaApi(reactiveApiFactory()))

  def psubscribe(keys: List[K], overflowStrategy: OverflowStrategy): Source[RedisResult[K, V], RedisSubscription] =
    subscribeInternal(keys, overflowStrategy, new RedisPSubscribeScalaApi(reactiveApiFactory()))

  private def subscribeInternal(
      keys: List[K],
      overflowStrategy: OverflowStrategy,
      reactiveApi: RedisReactiveScalaApi[K, V]
  ): Source[RedisResult[K, V], RedisSubscription] = {
    val connectionFuture = reactiveApi.subscribe(keys)
    val subscribeSource  = Source.fromFuture(connectionFuture)

    val redisKeyValueSource: Source[RedisResult[K, V], NotUsed] = reactiveApi.observe(overflowStrategy)

    subscribeSource
      .flatMapConcat(_ => redisKeyValueSource)
      .cancellable
      .watchTermination()(Keep.both)
      .mapMaterializedValue {
        case (killSwitch, terminationSignal) ⇒
          new RedisSubscriptionImpl(keys, connectionFuture, killSwitch, terminationSignal, reactiveApi)
      }
  }
}
