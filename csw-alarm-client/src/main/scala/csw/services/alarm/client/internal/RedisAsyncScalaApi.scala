package csw.services.alarm.client.internal

import io.lettuce.core.KeyValue
import io.lettuce.core.api.async.RedisAsyncCommands

import scala.async.Async.{async, await}
import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class RedisAsyncScalaApi[K, V](redisAsyncCommands: RedisAsyncCommands[K, V])(implicit ec: ExecutionContext) {
  def keys(key: K): Future[List[K]] = async {
    await(redisAsyncCommands.keys(key).toScala).asScala.toList
  }

  def mget(keys: List[K]): Future[List[KeyValue[K, V]]] = async {
    await(redisAsyncCommands.mget(keys: _*).toScala).asScala.toList
  }

  def get(key: K): Future[V] = async {
    await(redisAsyncCommands.get(key).toScala)
  }

  def set(key: K, value: V): Future[String] = async {
    await(redisAsyncCommands.set(key, value).toScala)
  }

  def setex(key: K, seconds: Long, value: V): Future[String] = async {
    await(redisAsyncCommands.setex(key, seconds, value).toScala)
  }
}
