/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package romaine.exceptions

case class RedisOperationFailed(msg: String, ex: Throwable = None.orNull) extends RuntimeException(msg, ex)
