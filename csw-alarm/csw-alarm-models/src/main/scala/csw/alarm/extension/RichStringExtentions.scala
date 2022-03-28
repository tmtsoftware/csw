/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.extension

import java.util.regex.Pattern

private[alarm] object RichStringExtentions {
  implicit class RichString(val value: String) extends AnyVal {
    def matches(pattern: Pattern): Boolean = pattern.matcher(value).matches()
    def isDefined: Boolean                 = value != null && !value.isEmpty
  }
}
