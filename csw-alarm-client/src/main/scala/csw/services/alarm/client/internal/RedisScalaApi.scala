package csw.services.alarm.client.internal

import io.lettuce.core.KeyValue
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.pubsub.api.reactive.{PatternMessage, RedisPubSubReactiveCommands}
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink.OverflowStrategy

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class RedisScalaApi[K, V](
    redisAsyncCommands: RedisAsyncCommands[K, V],
    redisReactiveCommands: RedisPubSubReactiveCommands[K, V]
)(implicit ec: ExecutionContext) {
  def set(key: K, value: V): Future[String]                  = redisAsyncCommands.set(key, value).toScala
  def setex(key: K, seconds: Long, value: V): Future[String] = redisAsyncCommands.setex(key, seconds, value).toScala
  def get(key: K): Future[V]                                 = redisAsyncCommands.get(key).toScala
  def mget(keys: List[K]): Future[List[KeyValue[K, V]]]      = redisAsyncCommands.mget(keys: _*).toScala.map(_.asScala.toList)
  def keys(key: K): Future[List[K]]                          = redisAsyncCommands.keys(key).toScala.map(_.asScala.toList)
  def psubscribe(keys: List[K]): Future[Unit]                = redisReactiveCommands.psubscribe(keys: _*).toFuture.toScala.map(_ ⇒ Unit)
  def observePatterns(overflowStrategy: OverflowStrategy): Flux[PatternMessage[K, V]] =
    redisReactiveCommands.observePatterns(overflowStrategy)
}
