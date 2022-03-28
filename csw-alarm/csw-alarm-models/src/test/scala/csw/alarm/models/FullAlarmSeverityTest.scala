/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.models

import csw.alarm.models.AlarmSeverity._
import csw.alarm.models.FullAlarmSeverity.Disconnected

// DEOPSCSW-437 : Model to represent alarm severities
class FullAlarmSeverityTest extends EnumTest(FullAlarmSeverity, "| DEOPSCSW-437") {
  override val expectedValues = Set(Disconnected, Okay, Warning, Major, Indeterminate, Critical)
}
