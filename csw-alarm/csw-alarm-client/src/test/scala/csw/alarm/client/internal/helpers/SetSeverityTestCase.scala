/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.client.internal.helpers

import csw.alarm.models.Key.AlarmKey
import csw.alarm.models.{AlarmSeverity, FullAlarmSeverity}

case class SetSeverityTestCase(
    alarmKey: AlarmKey,
    oldLatchedSeverity: FullAlarmSeverity,
    newSeverity: AlarmSeverity,
    expectedLatchedSeverity: FullAlarmSeverity,
    initializing: Boolean,
    expectedInitializing: Boolean
) {
  def name: String = alarmKey.name
}
