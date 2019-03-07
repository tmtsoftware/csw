package csw.alarm.client.internal.helpers

import csw.alarm.api.models.Key.AlarmKey
import csw.alarm.api.models.{AlarmSeverity, FullAlarmSeverity}

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
