package csw.services.alarm.client.internal.helpers

import csw.services.alarm.api.models.{AlarmSeverity, FullAlarmSeverity, LatchStatus}
import csw.services.alarm.api.models.Key.AlarmKey

case class SetSeverityTestCase(
    alarmKey: AlarmKey,
    latchableAlarm: Boolean,
    oldSeverity: FullAlarmSeverity,
    oldLatchStatus: LatchStatus,
    newSeverity: AlarmSeverity,
    expectedLatchedSeverity: FullAlarmSeverity,
    expectedLatchStatus: LatchStatus
) {
  override def toString: String = alarmKey.name
}
