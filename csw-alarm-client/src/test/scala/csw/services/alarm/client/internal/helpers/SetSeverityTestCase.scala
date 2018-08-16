package csw.services.alarm.client.internal.helpers

import csw.services.alarm.api.models.{AlarmSeverity, ExplicitAlarmSeverity}
import csw.services.alarm.api.models.Key.AlarmKey

case class SetSeverityTestCase(
    alarmKey: AlarmKey,
    oldSeverity: AlarmSeverity,
    newSeverity: ExplicitAlarmSeverity,
    expectedLatchedSeverity: AlarmSeverity,
) {
  override def toString: String = alarmKey.name
}
