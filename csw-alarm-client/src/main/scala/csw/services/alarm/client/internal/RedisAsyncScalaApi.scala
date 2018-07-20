package csw.services.alarm.client.internal

import io.lettuce.core.KeyValue
import io.lettuce.core.api.async.RedisAsyncCommands

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class RedisAsyncScalaApi[K, V](redisAsyncCommands: RedisAsyncCommands[K, V])(implicit ec: ExecutionContext) {
  def set(key: K, value: V): Future[String]                  = redisAsyncCommands.set(key, value).toScala
  def setex(key: K, seconds: Long, value: V): Future[String] = redisAsyncCommands.setex(key, seconds, value).toScala
  def get(key: K): Future[V]                                 = redisAsyncCommands.get(key).toScala
  def mget(keys: List[K]): Future[List[KeyValue[K, V]]]      = redisAsyncCommands.mget(keys: _*).toScala.map(_.asScala.toList)
  def keys(key: K): Future[List[K]]                          = redisAsyncCommands.keys(key).toScala.map(_.asScala.toList)
}
