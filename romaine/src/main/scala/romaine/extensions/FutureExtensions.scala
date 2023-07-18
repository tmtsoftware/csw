/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package romaine.extensions

import org.apache.pekko.Done
import romaine.exceptions.RedisOperationFailed

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

object FutureExtensions {
  implicit class RichFuture(response: Future[String]) {
    def failWith(reason: => String)(implicit ec: ExecutionContext): Future[Done] =
      response
        .map {
          case "OK" | "QUEUED" => Done
          case _               => throw RedisOperationFailed(reason)
        }
        .recoverWith { case NonFatal(ex) =>
          throw RedisOperationFailed(reason, ex)
        }
  }
}
