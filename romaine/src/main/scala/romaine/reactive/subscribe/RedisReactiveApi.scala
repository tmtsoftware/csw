/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package romaine.reactive.subscribe
import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import reactor.core.publisher.FluxSink.OverflowStrategy
import romaine.RedisResult

import scala.concurrent.Future

trait RedisReactiveApi[K, V] {
  def subscribe(keys: List[K]): Future[Done]
  def observe(overflowStrategy: OverflowStrategy): Source[RedisResult[K, V], NotUsed]
  def unsubscribe(keys: List[K]): Future[Done]
//  def close(): Future[Unit]
}
