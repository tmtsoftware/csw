package romaine.keyspace

import akka.stream.scaladsl.Source
import reactor.core.publisher.FluxSink.OverflowStrategy
import romaine.RedisOperation._
import romaine.async.RedisAsyncApi
import romaine.codec.RomaineStringCodec
import romaine.extensions.SourceExtensions.RichSource
import romaine.reactive.{RedisSubscription, RedisSubscriptionApi}
import romaine.{RedisOperation, RedisResult}

import scala.concurrent.{ExecutionContext, Future}

class RedisKeySpaceApi[K: RomaineStringCodec, V: RomaineStringCodec](
    redisSubscriptionApi: RedisSubscriptionApi[KeyspaceKey, RedisOperation],
    redisAsyncApi: RedisAsyncApi[K, V],
    keyspacePrefix: KeyspaceId = KeyspaceId._0
)(implicit ec: ExecutionContext) {

  def watchKeyspaceValue(
      keys: List[String],
      overflowStrategy: OverflowStrategy
  ): Source[RedisResult[K, Option[V]], RedisSubscription] =
    redisSubscriptionApi
      .psubscribe(keys.map(KeyspaceKey(keyspacePrefix, _)), overflowStrategy)
      .filterNot(_.value == Unknown)
      .mapAsync(1) { result ⇒
        val key = RomaineStringCodec[K].fromString(result.key.value)
        result.value match {
          case Set              ⇒ redisAsyncApi.get(key).map(valueOpt ⇒ (key, valueOpt))
          case Expired | Delete ⇒ Future.successful((key, None))
        }
      }
      .collect {
        case (k, v) ⇒ RedisResult(k, v)
      }
      .distinctUntilChanged

}
//todo: support for delete and expired, etc
//todo: RedisWatchSubscription try to remove type parameter
//todo: Fix exhaustive match warning
