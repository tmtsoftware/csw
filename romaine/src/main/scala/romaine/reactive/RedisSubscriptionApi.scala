package romaine.reactive

import akka.NotUsed
import akka.stream.scaladsl.{Keep, Source}
import reactor.core.publisher.FluxSink.OverflowStrategy
import romaine.extensions.SourceExtensions.RichSource

import scala.concurrent.ExecutionContext

class RedisSubscriptionApi[K, V](reactiveApiFactory: () => RedisReactiveScalaApi[K, V])(implicit ec: ExecutionContext) {
  def subscribe(
      keys: List[K],
      overflowStrategy: OverflowStrategy
  ): Source[RedisResult[K, V], RedisSubscription] = {
    val reactiveApi      = reactiveApiFactory()
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
