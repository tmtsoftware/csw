package romaine.keyspace

import akka.stream.scaladsl.Source
import reactor.core.publisher.FluxSink.OverflowStrategy
import romaine.RedisResult
import romaine.async.RedisAsyncApi
import romaine.codec.RomaineStringCodec
import romaine.extensions.SourceExtensions.RichSource
import romaine.keyspace.KeyspaceEvent.{Error, Updated, Wrapped}
import romaine.keyspace.RedisKeyspaceEvent.Unknown
import romaine.reactive.{RedisSubscription, RedisSubscriptionApi}

import scala.concurrent.{ExecutionContext, Future}

class RedisKeySpaceApi[K: RomaineStringCodec, V: RomaineStringCodec](
    redisSubscriptionApi: RedisSubscriptionApi[KeyspaceKey, RedisKeyspaceEvent],
    redisAsyncApi: RedisAsyncApi[K, V],
    keyspacePrefix: KeyspaceId = KeyspaceId._0
)(implicit ec: ExecutionContext) {

  def watchKeyspaceEvent(
      keys: List[K],
      overflowStrategy: OverflowStrategy
  ): Source[RedisResult[K, KeyspaceEvent[V]], RedisSubscription] =
    redisSubscriptionApi
      .psubscribe(keys.map(x ⇒ KeyspaceKey(keyspacePrefix, RomaineStringCodec[K].toString(x))), overflowStrategy)
      .mapAsync(1) { result ⇒
        val key = RomaineStringCodec[K].fromString(result.key.value)

        result.value match {
          case RedisKeyspaceEvent.Set ⇒
            redisAsyncApi.get(key).map { x ⇒
              if (x.isDefined) RedisResult(key, Updated(x.get))
              else RedisResult(key, Error(s"Received Set keyspace event for [$key] but value is not present in store."))
            }
          case event ⇒ Future.successful(RedisResult(key, Wrapped(event)))
        }
      }

  def watchKeyspaceValue(
      keys: List[K],
      overflowStrategy: OverflowStrategy
  ): Source[RedisResult[K, Option[V]], RedisSubscription] =
    watchKeyspaceEvent(keys, overflowStrategy)
      .collect {
        case RedisResult(k, Updated(v))                         ⇒ (k, Some(v))
        case RedisResult(k, Wrapped(event)) if event != Unknown ⇒ (k, None)
      }
      .map {
        case (k, v) ⇒ RedisResult(k, v)
      }
      .distinctUntilChanged

}
//todo: support for delete and expired, etc
//todo: RedisWatchSubscription try to remove type parameter
