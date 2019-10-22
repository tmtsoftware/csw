package romaine.keyspace

import akka.Done
import akka.stream.scaladsl.Source
import reactor.core.publisher.FluxSink.OverflowStrategy
import romaine.async.RedisAsyncApi
import romaine.codec.RomaineCodec
import romaine.codec.RomaineCodec.{FromString, ToBytesAndString}
import romaine.extensions.SourceExtensions.RichSource
import romaine.keyspace.KeyspaceEvent.{Error, Removed, Updated}
import romaine.keyspace.RedisKeyspaceEvent.{Delete, Expired, Unknown}
import romaine.reactive.{RedisSubscription, RedisSubscriptionApi}
import romaine.{RedisResult, RedisValueChange}

import scala.concurrent.{ExecutionContext, Future}

class RedisKeySpaceApi[K: RomaineCodec, V: RomaineCodec](
    redisSubscriptionApi: RedisSubscriptionApi[KeyspaceKey, RedisKeyspaceEvent],
    redisAsyncApi: RedisAsyncApi[K, V],
    keyspacePrefix: KeyspaceId = KeyspaceId._0
)(implicit ec: ExecutionContext) {

  def watchKeyspaceEvent(
      keys: List[K],
      overflowStrategy: OverflowStrategy
  ): Source[RedisResult[K, KeyspaceEvent[V]], RedisSubscription] =
    redisSubscriptionApi
      .psubscribe(keys.map(x => KeyspaceKey(keyspacePrefix, x.asString)), overflowStrategy)
      .mapAsync(1) { result =>
        val key = result.key.value.as[K]

        result.value match {
          case RedisKeyspaceEvent.Set =>
            redisAsyncApi.get(key).map { x =>
              if (x.isDefined) RedisResult(key, Updated(x.get))
              else RedisResult(key, Error(s"Received Set keyspace event for [$key] but value is not present in store."))
            }
          case Expired | Delete => Future.successful(RedisResult(key, Removed))
          case Unknown          => Future.successful(RedisResult(key, Error("Received unknown keyspace event.")))
        }
      }

  def watchKeyspaceValue(
      keys: List[K],
      overflowStrategy: OverflowStrategy
  ): Source[RedisResult[K, Option[V]], RedisSubscription] =
    watchKeyspaceEvent(keys, overflowStrategy).collect {
      case RedisResult(k, Updated(v)) => RedisResult[K, Option[V]](k, Some(v))
      case RedisResult(k, Removed)    => RedisResult[K, Option[V]](k, None)
    }.distinctUntilChanged

  def watchKeyspaceValueChange(
      keys: List[K],
      overflowStrategy: OverflowStrategy
  ): Source[RedisResult[K, RedisValueChange[Option[V]]], RedisSubscription] = {
    val initialValuesF: Future[Map[K, Option[V]]] = redisAsyncApi.mget(keys).map(_.map(x => x.key -> x.value).toMap)

    val sourceF = initialValuesF.map(initialValues => {
      watchKeyspaceValue(keys, overflowStrategy)
        .statefulMapConcat(() => {
          var digest = initialValues
          redisResult => {
            val change = RedisResult(
              redisResult.key,
              RedisValueChange(digest.get(redisResult.key).flatten, redisResult.value)
            )
            digest += redisResult.key -> redisResult.value
            List(change)
          }
        })
    })

    Source
      .futureSource(sourceF)
      .mapMaterializedValue(
        subscriptionF =>
          new RedisSubscription {
            override def unsubscribe(): Future[Done] = subscriptionF.flatMap(_.unsubscribe())
            override def ready(): Future[Done]       = subscriptionF.flatMap(_.ready())
          }
      )
  }
}
