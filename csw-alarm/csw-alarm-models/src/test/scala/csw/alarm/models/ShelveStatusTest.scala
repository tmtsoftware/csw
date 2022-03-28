/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.models

import csw.alarm.models.ShelveStatus.{Shelved, Unshelved}

// DEOPSCSW-442: Model to represent Alarm Shelve status
class ShelveStatusTest extends EnumTest(ShelveStatus, "| DEOPSCSW-442") {
  override val expectedValues = Set(Shelved, Unshelved)
}
