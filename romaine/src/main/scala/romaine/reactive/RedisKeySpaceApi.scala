package romaine.reactive

import akka.stream.scaladsl.Source
import reactor.core.publisher.FluxSink.OverflowStrategy
import romaine.async.RedisAsyncScalaApi
import romaine.codec.RomaineStringCodec
import romaine.extensions.SourceExtensions.RichSource

import scala.concurrent.{ExecutionContext, Future}

class RedisKeySpaceApi[K: RomaineStringCodec, V: RomaineStringCodec](
    redisSubscriptionApi: RedisSubscriptionApi[String, String],
    redisAsyncScalaApi: RedisAsyncScalaApi[K, V]
)(implicit ec: ExecutionContext) {

  private val SetOperation     = "set"
  private val ExpiredOperation = "expired"
  private val KeyspacePattern  = "__keyspace@0__:"

  def watchKeyspaceValue(
      keys: List[String],
      overflowStrategy: OverflowStrategy
  ): Source[RedisResult[K, Option[V]], RedisSubscription] = {

    redisSubscriptionApi
      .subscribe(keys.map(KeyspacePattern + _), overflowStrategy)
      .filter(pm => pm.value == SetOperation || pm.value == ExpiredOperation)
      .mapAsync(1) { pm =>
        val key = RomaineStringCodec[K].fromString(pm.key)
        pm.value match {
          case SetOperation     => redisAsyncScalaApi.get(key).map(valueOpt ⇒ (key, valueOpt))
          case ExpiredOperation => Future((key, None))
        }
      }
      .collect {
        case (k, v) ⇒ RedisResult(k, v)
      }
      .distinctUntilChanged
  }

  def watchKeyspaceValueAggregation(
      keys: List[String],
      overflowStrategy: OverflowStrategy,
      reducer: Iterable[Option[V]] => V
  ): Source[V, RedisSubscription] = {
    watchKeyspaceValue(keys, overflowStrategy)
      .scan(Map.empty[K, Option[V]]) {
        case (data, RedisResult(key, value)) ⇒ data + (key → value)
      }
      .map(data => reducer(data.values))
      .distinctUntilChanged
  }

  def watchKeyspaceField[TField](
      keys: List[String],
      overflowStrategy: OverflowStrategy,
      fieldMapper: V => TField
  ): Source[RedisResult[K, Option[TField]], RedisSubscription] = {
    val stream: Source[RedisResult[K, Option[TField]], RedisSubscription] =
      watchKeyspaceValue(keys, overflowStrategy).map {
        case RedisResult(k, Some(v)) => RedisResult(k, Some(fieldMapper(v)))
        case RedisResult(k, _)       => RedisResult(k, None)
      }
    stream.distinctUntilChanged
  }

  def watchKeyspaceFieldAggregation[TField](
      keys: List[String],
      overflowStrategy: OverflowStrategy,
      fieldMapper: V => TField,
      reducer: Iterable[Option[TField]] => TField
  ): Source[TField, RedisSubscription] = {
    watchKeyspaceField(keys, overflowStrategy, fieldMapper)
      .scan(Map.empty[K, Option[TField]]) {
        case (data, RedisResult(key, value)) ⇒ data + (key → value)
      }
      .map(data => reducer(data.values))
      .distinctUntilChanged
  }
}
//todo: support for delete and expired, etc
//todo: RedisWatchSubscription try to remove type parameter
