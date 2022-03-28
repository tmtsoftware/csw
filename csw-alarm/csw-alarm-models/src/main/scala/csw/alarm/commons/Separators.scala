/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.commons

private[alarm] object Separators {
  // Use "-" as separator since hyphen is an unsupported character in subsystem, component and alarm name
  // Which enables safe parsing of AlarmKey from string.
  val KeySeparator = '-'
}
