/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.api.internal

private[alarm] sealed trait ValidationResult

private[alarm] object ValidationResult {
  case object Success                       extends ValidationResult
  case class Failure(reasons: List[String]) extends ValidationResult
}
