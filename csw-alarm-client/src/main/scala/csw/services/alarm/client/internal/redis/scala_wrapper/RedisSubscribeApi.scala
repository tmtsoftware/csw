package csw.services.alarm.client.internal.redis.scala_wrapper

import akka.NotUsed
import akka.stream.scaladsl.{Keep, Source}
import csw.services.alarm.client.internal.extensions.SourceExtensions.RichSource
import reactor.core.publisher.FluxSink.OverflowStrategy

import scala.concurrent.ExecutionContext

class RedisSubscribeApi[TKey, TValue](
    subscribeApiFactory: () => RedisReactiveScalaApi[TKey, TValue]
)(implicit ec: ExecutionContext) {
  def subscribe(
      keys: List[TKey],
      overflowStrategy: OverflowStrategy
  ): Source[RedisResult[TKey, TValue], RedisSubscription[TKey]] = {
    val value            = subscribeApiFactory()
    val connectionFuture = value.subscribe(keys)
    val subscribeSource  = Source.fromFuture(connectionFuture)

    val redisKeyValueSource: Source[RedisResult[TKey, TValue], NotUsed] = {
      val channelSource = value.observeChannels(overflowStrategy)
      channelSource.map(x => RedisResult(x.getChannel, x.getMessage))
    }

    val finalStream = subscribeSource
      .flatMapConcat(_ => redisKeyValueSource)
      .cancellable
      .watchTermination()(Keep.both)
      .mapMaterializedValue {
        case (killSwitch, terminationSignal) ⇒
          new RedisSubscription(keys, connectionFuture, killSwitch, terminationSignal, value)
      }
    finalStream
  }
}
