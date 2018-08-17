package csw.services.alarm.client.internal.helpers

import csw.services.alarm.api.models.{AlarmSeverity, ExplicitAlarmSeverity, LatchStatus}
import csw.services.alarm.api.models.Key.AlarmKey

case class SetSeverityTestCase(
    alarmKey: AlarmKey,
    latchableAlarm: Boolean,
    oldSeverity: AlarmSeverity,
    oldLatchStatus: LatchStatus,
    newSeverity: ExplicitAlarmSeverity,
    expectedLatchedSeverity: AlarmSeverity,
    expectedLatchStatus: LatchStatus
) {
  override def toString: String = alarmKey.name
}
