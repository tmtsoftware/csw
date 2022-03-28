/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.command.client.internal

import scala.concurrent.duration.{DurationLong, FiniteDuration}

private[command] object Timeouts {
  val DefaultTimeout: FiniteDuration = 30.seconds
}
