package romaine.keyspace

import akka.stream.scaladsl.Source
import reactor.core.publisher.FluxSink.OverflowStrategy
import romaine.RedisResult
import romaine.async.RedisAsyncApi
import romaine.codec.RomaineStringCodec
import romaine.codec.RomaineStringCodec.{FromString, ToString}
import romaine.extensions.SourceExtensions.RichSource
import romaine.keyspace.KeyspaceEvent.{Error, Removed, Updated}
import romaine.keyspace.RedisKeyspaceEvent.{Delete, Expired, Unknown}
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
      .psubscribe(keys.map(x ⇒ KeyspaceKey(keyspacePrefix, x.asString)), overflowStrategy)
      .mapAsync(1) { result ⇒
        val key = result.key.value.as[K]

        result.value match {
          case RedisKeyspaceEvent.Set ⇒
            redisAsyncApi.get(key).map { x ⇒
              if (x.isDefined) RedisResult(key, Updated(x.get))
              else RedisResult(key, Error(s"Received Set keyspace event for [$key] but value is not present in store."))
            }
          case Expired | Delete ⇒ Future.successful(RedisResult(key, Removed))
          case Unknown          ⇒ Future.successful(RedisResult(key, Error("Received unknown keyspace event.")))
        }
      }

  def watchKeyspaceValue(
      keys: List[K],
      overflowStrategy: OverflowStrategy
  ): Source[RedisResult[K, Option[V]], RedisSubscription] =
    watchKeyspaceEvent(keys, overflowStrategy).collect {
      case RedisResult(k, Updated(v)) ⇒ RedisResult[K, Option[V]](k, Some(v))
      case RedisResult(k, Removed)    ⇒ RedisResult[K, Option[V]](k, None)
    }.distinctUntilChanged

}
