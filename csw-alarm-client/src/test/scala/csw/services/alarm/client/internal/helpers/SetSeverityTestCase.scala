package csw.services.alarm.client.internal.helpers

import csw.services.alarm.api.models.{AlarmSeverity, FullAlarmSeverity}
import csw.services.alarm.api.models.Key.AlarmKey

case class SetSeverityTestCase(
    alarmKey: AlarmKey,
    oldLatchedSeverity: FullAlarmSeverity,
    newSeverity: AlarmSeverity,
    expectedLatchedSeverity: FullAlarmSeverity
) {
  def name: String = alarmKey.name
}
