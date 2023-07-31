/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.models

import csw.alarm.models.AlarmSeverity.*

class AlarmSeverityTest extends EnumTest(AlarmSeverity, "") {
  override val expectedValues = Set(Indeterminate, Okay, Warning, Major, Critical)
}
