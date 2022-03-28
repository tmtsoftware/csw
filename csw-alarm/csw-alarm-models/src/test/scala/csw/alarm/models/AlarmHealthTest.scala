/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.models

import csw.alarm.models.AlarmHealth._
import csw.alarm.models.AlarmSeverity._
import csw.alarm.models.FullAlarmSeverity.Disconnected

// DEOPSCSW-458: Calculate component health based on alarm severities
class AlarmHealthTest extends EnumTest(AlarmHealth, "| DEOPSCSW-458") {
  override val expectedValues = Set(Good, Ill, Bad)

  test("severities Okay, Warning should result in Good health | DEOPSCSW-458") {
    AlarmHealth.fromSeverity(Okay) shouldBe Good
    AlarmHealth.fromSeverity(Warning) shouldBe Good
  }

  test("severity Major should result in Ill health | DEOPSCSW-458") {
    AlarmHealth.fromSeverity(Major) shouldBe Ill
  }

  test("severities Disconnected, Indeterminate, Critical should result in Bad health | DEOPSCSW-458") {
    AlarmHealth.fromSeverity(Disconnected) shouldBe Bad
    AlarmHealth.fromSeverity(Indeterminate) shouldBe Bad
    AlarmHealth.fromSeverity(Critical) shouldBe Bad
  }
}
