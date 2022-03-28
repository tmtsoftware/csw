/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.services.internal

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{Await, Future}

object FutureExt {
  implicit class FutureOps[T](private val f: Future[T]) extends AnyVal {
    def await(duration: FiniteDuration = 5.seconds): T = Await.result(f, duration)
  }
}
