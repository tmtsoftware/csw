package romaine.reactive

import akka.NotUsed
import akka.stream.scaladsl.{Keep, Source}
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import reactor.core.publisher.FluxSink.OverflowStrategy
import romaine.exceptions.RedisServerNotAvailable
import romaine.extensions.SourceExtensions.RichSource

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class RedisSubscriptionApi[K, V](reactiveApiFactory: () => Future[RedisPubSubReactiveCommands[K, V]])(
    implicit ec: ExecutionContext
) {
  def subscribe(keys: List[K], overflowStrategy: OverflowStrategy): Source[RedisResult[K, V], RedisSubscription] =
    subscribeInternal(keys, overflowStrategy, () => reactiveApiFactory().map(x => new RedisSubscribeApi(x)))

  def psubscribe(keys: List[K], overflowStrategy: OverflowStrategy): Source[RedisResult[K, V], RedisSubscription] =
    subscribeInternal(keys, overflowStrategy, () => reactiveApiFactory().map(x => new RedisPSubscribeApi(x)))

  private def subscribeInternal(
      keys: List[K],
      overflowStrategy: OverflowStrategy,
      reactiveApiFactory: () => Future[RedisReactiveApi[K, V]]
  ): Source[RedisResult[K, V], RedisSubscription] = {
    val reactiveApiF                                                    = init(reactiveApiFactory)
    val futureSource                                                    = reactiveApiF.map(_.observe(overflowStrategy))
    val redisKeyValueSource: Source[RedisResult[K, V], Future[NotUsed]] = Source.fromFutureSource(futureSource)
    val connectedF                                                      = futureSource.flatMap(_ => reactiveApiF.flatMap(_.subscribe(keys)))

    redisKeyValueSource.cancellable
      .watchTermination()(Keep.both)
      .mapMaterializedValue {
        case (killSwitch, terminationSignal) ⇒
          new RedisSubscriptionImpl(keys, connectedF, killSwitch, terminationSignal, reactiveApiF)
      }
  }

  private def init[T](conn: () ⇒ Future[T]): Future[T] = {
    Future.unit.flatMap(_ => conn()).recover {
      case NonFatal(ex) ⇒ throw RedisServerNotAvailable(ex.getCause)
    }
  }
}
