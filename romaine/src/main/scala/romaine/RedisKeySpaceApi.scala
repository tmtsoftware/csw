package romaine

import akka.NotUsed
import akka.stream.scaladsl.{Keep, Source}
import reactor.core.publisher.FluxSink.OverflowStrategy
import romaine.extensions.SourceExtensions.RichSource

import scala.concurrent.{ExecutionContext, Future}

class RedisKeySpaceApi[TKey, TValue](
    redisReactiveScalaApi: RedisReactiveScalaApi[String, String],
    redisAsyncScalaApi: RedisAsyncScalaApi[TKey, TValue]
)(implicit redisKeySpaceCodec: RedisKeySpaceCodec[TKey, TValue], ec: ExecutionContext) {

  def watchKeyspaceValue(
      keys: List[String],
      overflowStrategy: OverflowStrategy
  ): Source[RedisResult[TKey, TValue], RedisSubscription[String]] = {

    val patternBasedKeys = keys.map("__keyspace@0__:" + _)

    val pSubscribeF: Future[Unit] = redisReactiveScalaApi.psubscribe(patternBasedKeys)

    val pSubscribeSource = Source.fromFuture(pSubscribeF)

    val redisKeyValueSource: Source[RedisResult[TKey, TValue], NotUsed] = {
      val patternSource = redisReactiveScalaApi.observePatterns(overflowStrategy)
      patternSource
        .filter(pm => pm.getMessage == "set")
        .mapAsync(1) { pm =>
          val key = redisKeySpaceCodec.fromKeyString(pm.getChannel)
          redisAsyncScalaApi.get(key).map(valueOpt ⇒ valueOpt.map(value ⇒ (key, value)))
        }
        .collect {
          case Some((k, v)) ⇒ RedisResult(k, v)
        }
        .distinctUntilChanged
    }

    pSubscribeSource
      .flatMapConcat(_ => redisKeyValueSource)
      .cancellable
      .watchTermination()(Keep.both)
      .mapMaterializedValue {
        case (killSwitch, terminationSignal) ⇒
          new RedisSubscription(keys, pSubscribeF, killSwitch, terminationSignal, redisReactiveScalaApi)
      }
  }

  def watchKeyspaceValueAggregation(
      keys: List[String],
      overflowStrategy: OverflowStrategy,
      reducer: Iterable[TValue] => TValue
  ): Source[TValue, RedisSubscription[String]] = {
    watchKeyspaceValue(keys, overflowStrategy)
      .scan(Map.empty[TKey, TValue]) {
        case (data, RedisResult(key, value)) ⇒ data + (key → value)
      }
      .map(data => reducer(data.values))
      .distinctUntilChanged
  }

  def watchKeyspaceField[TField](
      keys: List[String],
      overflowStrategy: OverflowStrategy,
      fieldMapper: TValue => TField
  ): Source[RedisResult[TKey, TField], RedisSubscription[String]] = {
    watchKeyspaceValue(keys, overflowStrategy)
      .map(x => RedisResult(x.key, fieldMapper(x.value)))
      .distinctUntilChanged
  }

  def watchKeyspaceFieldAggregation[TField](
      keys: List[String],
      overflowStrategy: OverflowStrategy,
      fieldMapper: TValue => TField,
      reducer: Iterable[TField] => TField
  ): Source[TField, RedisSubscription[String]] = {
    watchKeyspaceField(keys, overflowStrategy, fieldMapper)
      .scan(Map.empty[TKey, TField]) {
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
