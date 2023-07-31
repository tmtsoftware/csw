/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package romaine

import io.lettuce.core.{RedisClient, RedisURI}
import romaine.async.RedisAsyncApi
import romaine.codec.{RomaineCodec, RomaineRedisCodec}
import romaine.exceptions.RedisServerNotAvailable
import romaine.reactive.RedisSubscriptionApi

import scala.jdk.FutureConverters.*
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class RomaineFactory(redisClient: RedisClient)(implicit val ec: ExecutionContext) {
  def redisAsyncApi[K: RomaineCodec, V: RomaineCodec](redisURIF: Future[RedisURI]): RedisAsyncApi[K, V] =
    new RedisAsyncApi(
      cps.async {
        val redisURI    = cps.await(redisURIF)
        val connectionF = init { () => redisClient.connectAsync(new RomaineRedisCodec[K, V], redisURI).asScala }
        cps.await(connectionF).async()
      }
    )

  def redisSubscriptionApi[K: RomaineCodec, V: RomaineCodec](redisURIF: Future[RedisURI]): RedisSubscriptionApi[K, V] =
    new RedisSubscriptionApi(() =>
      cps.async {
        val redisURI    = cps.await(redisURIF)
        val connectionF = init { () => redisClient.connectPubSubAsync(new RomaineRedisCodec[K, V], redisURI).asScala }
        cps.await(connectionF).reactive()
      }
    )

  private def init[T](conn: () => Future[T]): Future[T] =
    Future.unit.flatMap(_ => conn()).recover { case NonFatal(ex) =>
      throw RedisServerNotAvailable(ex.getCause)
    }
}
