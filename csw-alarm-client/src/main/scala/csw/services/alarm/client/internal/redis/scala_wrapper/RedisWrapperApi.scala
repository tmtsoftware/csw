package csw.services.alarm.client.internal.redis.scala_wrapper

import akka.Done
import akka.stream.KillSwitch
import akka.stream.scaladsl.{Keep, Source}
import csw.services.alarm.client.internal.extensions.SourceExtensions.RichSource
import reactor.core.publisher.FluxSink.OverflowStrategy

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

class RedisWrapperApi[TPatternKey, TKey, TValue](
    redisReactiveScalaApi: RedisReactiveScalaApi[TPatternKey, String],
    redisAsyncScalaApi: RedisAsyncScalaApi[TKey, TValue],
    convertKey: TPatternKey => TKey
)(implicit ec: ExecutionContext) {

  def watchValue(keys: List[TPatternKey],
                 overflowStrategy: OverflowStrategy): Source[RedisWatchResult[TKey, TValue], RedisWatchSubscription] = {
    val pSubscribeF: Future[Unit] = redisReactiveScalaApi.psubscribe(keys)

    val pSubsribeSource = Source.fromFuture(pSubscribeF)

    val eventStream = {
      val patternSource = redisReactiveScalaApi.observePatterns(overflowStrategy)
      patternSource
        .filter(pm => pm.getMessage == "set")
        .mapAsync(1)(pm => {
          val convertedKey = convertKey(pm.getChannel)
          val value        = redisAsyncScalaApi.get(convertedKey)
          value.map(v => (convertedKey, v))
        })
        .map({
          case (k, v) => RedisWatchResult(k, v)
        })
        .distinctUntilChanged
    }

    val finalStream = pSubsribeSource.flatMapConcat { _ =>
      eventStream
    }.cancellable

    mapMatToSubscription(finalStream, keys, pSubscribeF)
  }

  def watchValueAggregation[TValue](
      keys: List[TPatternKey],
      overflowStrategy: OverflowStrategy,
      reducer: Iterable[TValue] => TValue
  ): Source[TValue, RedisWatchSubscription] = {
    watchValue(keys, overflowStrategy)
      .scan(Map.empty[TKey, TValue]) {
        case (data, RedisWatchResult(key, value)) ⇒ data + (key → value)
      }
      .map(data => reducer(data.values))
      .distinctUntilChanged
  }

  def watchField[TField](keys: List[TPatternKey],
                         overflowStrategy: OverflowStrategy,
                         fieldMapper: TValue => TField): Source[RedisWatchResult[TKey, TField], RedisWatchSubscription] = {
    watchValue(keys, overflowStrategy)
      .map(x => RedisWatchResult(x.key, fieldMapper(x.value)))
      .distinctUntilChanged
  }

  def watchFieldAggregation[TField](
      keys: List[TPatternKey],
      overflowStrategy: OverflowStrategy,
      fieldMapper: TValue => TField,
      reducer: Iterable[TField] => TField
  ): Source[TField, RedisWatchSubscription[TPatternKey]] = {
    watchField(keys, overflowStrategy, fieldMapper)
      .scan(Map.empty[TKey, TField]) {
        case (data, RedisWatchResult(key, value)) ⇒ data + (key → value)
      }
      .map(data => reducer(data.values))
      .distinctUntilChanged
  }

  private def mapMatToSubscription[TSource](source: Source[TSource, KillSwitch],
                                            keys: List[TPatternKey],
                                            pSubscribeF: Future[Unit]) = {
    source
      .watchTermination()(Keep.both)
      .mapMaterializedValue {
        case (killSwitch, terminationSignal) ⇒
          new RedisWatchSubscription(keys, pSubscribeF, killSwitch, terminationSignal, redisReactiveScalaApi)
      }
  }
}

case class RedisWatchResult[K, V](key: K, value: V)

//distinct events - DONE
//subscriptionResult with unsubscribe and quit - DONE
//todo: support for delete and expire
//watch termination - DONE

class RedisWatchSubscription[TPatternKey](
    keys: List[TPatternKey],
    pSubscribeF: Future[Unit],
    killSwitch: KillSwitch,
    terminationSignal: Future[Done],
    redisReactiveScalaApi: RedisReactiveScalaApi[TPatternKey, _]
)(implicit executionContext: ExecutionContext) {

  /**
   * To unsubscribe a given subscription. This will also clean up subscription specific underlying resources
   * @return a future which completes when the unsubscribe is completed
   */
  def unsubscribe(): Future[Unit] = async {
    await(redisReactiveScalaApi.punsubscribe(keys))
    await(redisReactiveScalaApi.quit)
    killSwitch.shutdown()
    await(terminationSignal) // await on terminationSignal when unsubscribe is called by user
  }

  /**
   * To check if the underlying subscription is ready to emit elements
   * @return a future which completes when the underlying subscription is ready to emit elements
   */
  def ready(): Future[Unit] = async {
    case _ if terminationSignal.isCompleted ⇒ terminationSignal.map(_ ⇒ ())
  }
}
