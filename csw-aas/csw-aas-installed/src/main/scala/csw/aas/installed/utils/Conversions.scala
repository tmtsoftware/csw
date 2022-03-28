/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.aas.installed.utils

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object Conversions {
  implicit class RichEitherTFuture[A, B](futureEither: Future[Either[A, B]]) {
    def block(): Either[A, B] = Await.result(futureEither, 5.seconds)
  }
}
