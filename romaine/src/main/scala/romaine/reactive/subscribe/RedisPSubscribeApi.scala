package romaine.reactive.subscribe

import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import reactor.core.publisher.FluxSink.OverflowStrategy
import romaine.RedisResult

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class RedisPSubscribeApi[K, V](redisReactiveCommands: RedisPubSubReactiveCommands[K, V])(implicit ec: ExecutionContext)
    extends RedisReactiveApi[K, V] {
  def subscribe(keys: List[K]): Future[Done] = redisReactiveCommands.psubscribe(keys: _*).toFuture.toScala.map(_ ⇒ Done)

  def observe(overflowStrategy: OverflowStrategy): Source[RedisResult[K, V], NotUsed] =
    Source
      .fromPublisher(redisReactiveCommands.observePatterns(overflowStrategy))
      .map(x => RedisResult(x.getChannel, x.getMessage))

  def unsubscribe(keys: List[K]): Future[Done] = redisReactiveCommands.punsubscribe(keys: _*).toFuture.toScala.map(_ ⇒ Done)
  def quit(): Future[String]                   = redisReactiveCommands.quit().toFuture.toScala
}
