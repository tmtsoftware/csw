/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package romaine.reactive.subscribe

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.{Done, NotUsed}
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import reactor.core.publisher.FluxSink.OverflowStrategy
import romaine.RedisResult

import scala.jdk.FutureConverters.*
import scala.concurrent.{blocking, ExecutionContext, Future}

class RedisSubscribeApi[K, V](redisReactiveCommands: RedisPubSubReactiveCommands[K, V])(implicit ec: ExecutionContext)
    extends RedisReactiveApi[K, V] {
  def subscribe(keys: List[K]): Future[Done] = redisReactiveCommands.subscribe(keys: _*).toFuture.asScala.map(_ => Done)
  def observe(overflowStrategy: OverflowStrategy): Source[RedisResult[K, V], NotUsed] =
    Source
      .fromPublisher(redisReactiveCommands.observeChannels(overflowStrategy))
      .map(x => RedisResult(x.getChannel, x.getMessage))
  def unsubscribe(keys: List[K]): Future[Done] = redisReactiveCommands.unsubscribe(keys: _*).toFuture.asScala.map(_ => Done)
  def close(): Future[Unit] =
    Future {
      blocking {
        redisReactiveCommands.getStatefulConnection.close()
      }
    }
}
