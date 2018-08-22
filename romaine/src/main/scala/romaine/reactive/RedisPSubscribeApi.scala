package romaine.reactive

import akka.NotUsed
import akka.stream.scaladsl.Source
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import reactor.core.publisher.FluxSink.OverflowStrategy

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class RedisPSubscribeApi[K, V](redisReactiveCommands: RedisPubSubReactiveCommands[K, V])(implicit ec: ExecutionContext)
    extends RedisReactiveApi[K, V] {
  def subscribe(keys: List[K]): Future[Unit] = redisReactiveCommands.psubscribe(keys: _*).toFuture.toScala.map(_ ⇒ ())
  def observe(overflowStrategy: OverflowStrategy): Source[RedisResult[K, V], NotUsed] =
    Source
      .fromPublisher(redisReactiveCommands.observePatterns(overflowStrategy))
      .map(x => RedisResult(x.getChannel, x.getMessage))
  def unsubscribe(keys: List[K]): Future[Unit] = redisReactiveCommands.punsubscribe(keys: _*).toFuture.toScala.map(_ ⇒ ())
  def quit(): Future[String]                   = redisReactiveCommands.quit().toFuture.toScala
}
