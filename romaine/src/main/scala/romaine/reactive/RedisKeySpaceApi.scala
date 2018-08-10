package romaine.reactive

import akka.stream.scaladsl.Source
import reactor.core.publisher.FluxSink.OverflowStrategy
import romaine._
import romaine.extensions.SourceExtensions.RichSource

import scala.concurrent.ExecutionContext

class RedisKeySpaceApi[K, V](
    redisPSubscribeApi: RedisPSubscribeScalaApi[String, String],
    redisAsyncScalaApi: RedisAsyncScalaApi[K, V]
)(implicit redisKeySpaceCodec: RedisKeySpaceCodec[K, V], ec: ExecutionContext) {

  private val redisSubscriptionApi: RedisSubscriptionApi[String, String] = new RedisSubscriptionApi(() => redisPSubscribeApi)

  def watchKeyspaceValue(
      keys: List[String],
      overflowStrategy: OverflowStrategy
  ): Source[RedisResult[K, V], RedisSubscription[String]] =
    redisSubscriptionApi
      .subscribe(keys.map("__keyspace@0__:" + _), overflowStrategy)
      .filter(pm => pm.value == "set")
      .mapAsync(1) { pm =>
        val key = redisKeySpaceCodec.fromKeyString(pm.key)
        redisAsyncScalaApi.get(key).map(valueOpt ⇒ valueOpt.map(value ⇒ (key, value)))
      }
      .collect {
        case Some((k, v)) ⇒ RedisResult(k, v)
      }
      .distinctUntilChanged

  def watchKeyspaceValueAggregation(
      keys: List[String],
      overflowStrategy: OverflowStrategy,
      reducer: Iterable[V] => V
  ): Source[V, RedisSubscription[String]] = {
    watchKeyspaceValue(keys, overflowStrategy)
      .scan(Map.empty[K, V]) {
        case (data, RedisResult(key, value)) ⇒ data + (key → value)
      }
      .map(data => reducer(data.values))
      .distinctUntilChanged
  }

  def watchKeyspaceField[TField](
      keys: List[String],
      overflowStrategy: OverflowStrategy,
      fieldMapper: V => TField
  ): Source[RedisResult[K, TField], RedisSubscription[String]] = {
    watchKeyspaceValue(keys, overflowStrategy)
      .map(x => RedisResult(x.key, fieldMapper(x.value)))
      .distinctUntilChanged
  }

  def watchKeyspaceFieldAggregation[TField](
      keys: List[String],
      overflowStrategy: OverflowStrategy,
      fieldMapper: V => TField,
      reducer: Iterable[TField] => TField
  ): Source[TField, RedisSubscription[String]] = {
    watchKeyspaceField(keys, overflowStrategy, fieldMapper)
      .scan(Map.empty[K, TField]) {
        case (data, RedisResult(key, value)) ⇒ data + (key → value)
      }
      .map(data => reducer(data.values))
      .distinctUntilChanged
  }
}
//distinct events - DONE
//subscriptionResult with unsubscribe and quit - DONE
//watch termination - DONE
//remove aggregate key - DONE
//todo: support for delete and expired, etc
//todo: RedisWatchSubscription try to remove type parameter
