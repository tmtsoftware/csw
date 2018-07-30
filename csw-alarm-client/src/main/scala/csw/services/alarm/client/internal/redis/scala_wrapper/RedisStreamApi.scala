package csw.services.alarm.client.internal.redis.scala_wrapper

import akka.NotUsed
import akka.stream.KillSwitch
import akka.stream.scaladsl.{Keep, Source}
import csw.services.alarm.client.internal.extensions.SourceExtensions.RichSource
import reactor.core.publisher.FluxSink.OverflowStrategy

import scala.concurrent.{ExecutionContext, Future}

class RedisStreamApi[TKey, TValue](
    redisReactiveScalaApi: RedisReactiveScalaApi[String, String],
    redisAsyncScalaApi: RedisAsyncScalaApi[TKey, TValue],
)(implicit redisKeySpaceCodec: RedisKeySpaceCodec[TKey, TValue], ec: ExecutionContext) {

  def watchValue(
      keys: List[String],
      overflowStrategy: OverflowStrategy
  ): Source[RedisWatchResult[TKey, TValue], RedisWatchSubscription[String]] = {

    val patternBasedKeys = keys.map("__keyspace@0__:" + _)

    val pSubscribeF: Future[Unit] = redisReactiveScalaApi.psubscribe(patternBasedKeys)

    val pSubsribeSource = Source.fromFuture(pSubscribeF)

    val redisKeyValueSouce: Source[RedisWatchResult[TKey, TValue], NotUsed] = {
      val patternSource = redisReactiveScalaApi.observePatterns(overflowStrategy)
      patternSource
        .filter(pm => pm.getMessage == "set")
        .mapAsync(1)(pm => {
          val key   = redisKeySpaceCodec.fromKeyString(pm.getChannel)
          val value = redisAsyncScalaApi.get(key)
          value.map(v => (key, v))
        })
        .map({
          case (k, v) => RedisWatchResult(k, v)
        })
        .distinctUntilChanged
    }

    val finalStream = pSubsribeSource.flatMapConcat { _ =>
      redisKeyValueSouce
    }.cancellable

    mapMatToSubscription(finalStream, keys, pSubscribeF)
  }

  def watchValueAggregation(
      keys: List[String],
      overflowStrategy: OverflowStrategy,
      reducer: Iterable[TValue] => TValue
  ): Source[TValue, RedisWatchSubscription[String]] = {
    watchValue(keys, overflowStrategy)
      .scan(Map.empty[TKey, TValue]) {
        case (data, RedisWatchResult(key, value)) ⇒ data + (key → value)
      }
      .map(data => reducer(data.values))
      .distinctUntilChanged
  }

  def watchField[TField](
      keys: List[String],
      overflowStrategy: OverflowStrategy,
      fieldMapper: TValue => TField
  ): Source[RedisWatchResult[TKey, TField], RedisWatchSubscription[String]] = {
    watchValue(keys, overflowStrategy)
      .map(x => RedisWatchResult(x.key, fieldMapper(x.value)))
      .distinctUntilChanged
  }

  def watchFieldAggregation[TField](
      keys: List[String],
      overflowStrategy: OverflowStrategy,
      fieldMapper: TValue => TField,
      reducer: Iterable[TField] => TField
  ): Source[TField, RedisWatchSubscription[String]] = {
    watchField(keys, overflowStrategy, fieldMapper)
      .scan(Map.empty[TKey, TField]) {
        case (data, RedisWatchResult(key, value)) ⇒ data + (key → value)
      }
      .map(data => reducer(data.values))
      .distinctUntilChanged
  }

  private def mapMatToSubscription[TSource](source: Source[TSource, KillSwitch],
                                            keys: List[String],
                                            pSubscribeF: Future[Unit]) = {
    source
      .watchTermination()(Keep.both)
      .mapMaterializedValue {
        case (killSwitch, terminationSignal) ⇒
          new RedisWatchSubscription(keys, pSubscribeF, killSwitch, terminationSignal, redisReactiveScalaApi)
      }
  }
}
//distinct events - DONE
//subscriptionResult with unsubscribe and quit - DONE
//watch termination - DONE
//todo: remove aggregate key
//todo: support for delete and expired, etc
