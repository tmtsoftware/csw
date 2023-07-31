/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package romaine.async

import org.apache.pekko.Done
import io.lettuce.core.api.async.RedisAsyncCommands
import romaine.RedisResult
import romaine.exceptions.RedisServerNotAvailable
import romaine.extensions.FutureExtensions.RichFuture

import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*
import scala.jdk.OptionConverters.*

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class RedisAsyncApi[K, V](redisAsyncCommands: Future[RedisAsyncCommands[K, V]])(implicit ec: ExecutionContext) {

  def set(key: K, value: V): Future[Done] =
    redisAsyncCommands.flatMap(_.set(key, value).asScala.failWith(s"Redis 'SET' operation failed for [key:$key value:$value]"))

  def mset(map: Map[K, V]): Future[Done] =
    redisAsyncCommands.flatMap(_.mset(map.asJava).asScala.failWith(s"Redis 'MSET' operation failed for [map: $map]"))

  def setex(key: K, seconds: Long, value: V): Future[Done] =
    redisAsyncCommands.flatMap(
      _.setex(key, seconds, value).asScala.failWith(s"Redis 'SETEX' operation failed for [key: $key, value: $value]")
    )

  def get(key: K): Future[Option[V]] =
    redisAsyncCommands.flatMap(_.get(key).asScala.map(Option(_))).recover { case NonFatal(ex) =>
      throw RedisServerNotAvailable(ex.getCause)
    }

  def mget(keys: List[K]): Future[List[RedisResult[K, Option[V]]]] =
    redisAsyncCommands.flatMap(
      _.mget(keys *).asScala
        .map(_.asScala.map(kv => RedisResult(kv.getKey, kv.optional().toScala)).toList)
    )

  def keys(key: K): Future[List[K]] = redisAsyncCommands.flatMap(_.keys(key).asScala.map(_.asScala.toList))

  def exists(keys: K*): Future[Boolean] = redisAsyncCommands.flatMap(_.exists(keys *).asScala.map(_ == keys.size))

  def del(keys: List[K]): Future[Long] = redisAsyncCommands.flatMap(_.del(keys *).asScala.map(_.toLong))

  def pdel(pattern: K): Future[Long] =
    keys(pattern).flatMap(matchedKeys => if (matchedKeys.nonEmpty) del(matchedKeys) else Future.successful(0))

  def publish(key: K, value: V): Future[Long] = redisAsyncCommands.flatMap(_.publish(key, value).asScala.map(_.toLong))

  def quit(): Future[String] = redisAsyncCommands.flatMap(_.quit().asScala)
}
