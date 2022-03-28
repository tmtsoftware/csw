/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package romaine.exceptions

case class RedisServerNotAvailable(cause: Throwable) extends RuntimeException("Redis Server not available", cause)
