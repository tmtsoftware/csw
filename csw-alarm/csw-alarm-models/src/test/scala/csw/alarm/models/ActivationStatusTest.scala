/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.models

import csw.alarm.models.ActivationStatus.{Active, Inactive}

// DEOPSCSW-443: Model to represent Alarm Activation status
class ActivationStatusTest extends EnumTest(ActivationStatus, "| DEOPSCSW-443") {
  override val expectedValues = Set(Active, Inactive)
}
