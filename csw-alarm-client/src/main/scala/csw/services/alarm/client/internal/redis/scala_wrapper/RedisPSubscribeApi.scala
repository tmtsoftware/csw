package csw.services.alarm.client.internal.redis.scala_wrapper

import akka.NotUsed
import akka.stream.scaladsl.{Keep, Source}
import csw.services.alarm.client.internal.extensions.SourceExtensions.RichSource
import reactor.core.publisher.FluxSink.OverflowStrategy

import scala.concurrent.ExecutionContext

class RedisPSubscribeApi[TKey, TValue](
    psubscribeApiFactory: () => RedisReactiveScalaApi[String, TValue]
)(
    implicit redisKeySpaceCodec: RedisKeySpaceCodec[TKey, TValue],
    ec: ExecutionContext
) {
  def pSubscribe(
      keys: List[String],
      overflowStrategy: OverflowStrategy
  ): Source[RedisResult[TKey, TValue], RedisSubscription[String]] = {
    val value            = psubscribeApiFactory()
    val connectionFuture = value.psubscribe(keys)
    val psubscribeSource = Source.fromFuture(connectionFuture)

    val redisKeyValueSource: Source[RedisResult[TKey, TValue], NotUsed] = {
      val channelSource = value.observePatterns(overflowStrategy)
      channelSource.map(x => RedisResult(redisKeySpaceCodec.fromKeyString(x.getChannel), x.getMessage))
    }

    val finalStream = psubscribeSource
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
