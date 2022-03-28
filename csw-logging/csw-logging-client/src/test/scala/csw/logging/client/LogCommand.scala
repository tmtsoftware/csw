/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.logging.client

sealed trait LogCommand
object LogCommand {
  case object LogTrace extends LogCommand
  case object LogDebug extends LogCommand
  case object LogInfo  extends LogCommand
  case object LogWarn  extends LogCommand
  case object LogError extends LogCommand
  case object LogFatal extends LogCommand
  case object Unknown  extends LogCommand
}
