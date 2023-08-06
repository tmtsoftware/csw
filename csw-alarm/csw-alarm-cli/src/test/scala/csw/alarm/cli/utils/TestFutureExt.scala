/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.cli.utils

import scala.concurrent.duration.{Duration, DurationDouble}
import scala.concurrent.{Await, Future}

object TestFutureExt {
  class RichFuture[T](f: Future[T]) {
    def await(duration: Duration): T = Await.result(f, duration)
    def await: T                     = await(10.seconds)
  }

  given futureConversion[T]: Conversion[Future[T], RichFuture[T]] with
    def apply(x: Future[T]): RichFuture[T] = RichFuture(x)
}
