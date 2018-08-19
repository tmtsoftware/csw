package romaine.async

import io.lettuce.core.KeyValue
import io.lettuce.core.api.async.RedisAsyncCommands
import romaine.extensions.FutureExtensions.RichFuture

import scala.collection.JavaConverters.{iterableAsScalaIterableConverter, mapAsJavaMapConverter}
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class RedisAsyncApi[K, V](redisAsyncCommands: RedisAsyncCommands[K, V])(implicit ec: ExecutionContext) {
  def set(key: K, value: V): Future[Unit] =
    redisAsyncCommands.set(key, value).toScala.failWith(s"Redis 'SET' operation failed for [key:$key value:$value]")

  def mset(map: Map[K, V]): Future[Unit] =
    redisAsyncCommands
      .mset(map.asJava)
      .toScala
      .failWith(s"Redis 'MSET' operation failed for [map: $map]")

  def setex(key: K, seconds: Long, value: V): Future[Unit] =
    redisAsyncCommands
      .setex(key, seconds, value)
      .toScala
      .failWith(s"Redis 'SETEX' operation failed for [key: $key, value: $value]")

  def get(key: K): Future[Option[V]] = redisAsyncCommands.get(key).toScala.map(Option(_))

  def mget(keys: List[K]): Future[List[KeyValue[K, V]]] = redisAsyncCommands.mget(keys: _*).toScala.map(_.asScala.toList)

  def keys(key: K): Future[List[K]] = redisAsyncCommands.keys(key).toScala.map(_.asScala.toList)

  def exists(keys: K*): Future[Boolean] = redisAsyncCommands.exists(keys: _*).toScala.map(_ == keys.size)

  def del(keys: List[K]): Future[Long] = redisAsyncCommands.del(keys: _*).toScala.map(_.toLong)

  def pdel(pattern: K): Future[Long] =
    keys(pattern).flatMap(matchedKeys ⇒ if (matchedKeys.nonEmpty) del(matchedKeys) else Future.successful(0))

  def publish(key: K, value: V): Future[Long] = redisAsyncCommands.publish(key, value).toScala.map(_.toLong)

  def quit(): Future[String] = redisAsyncCommands.quit().toScala
}
