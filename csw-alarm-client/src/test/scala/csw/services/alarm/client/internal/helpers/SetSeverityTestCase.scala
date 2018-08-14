package csw.services.alarm.client.internal.helpers

import csw.services.alarm.api.models.AlarmSeverity
import csw.services.alarm.api.models.Key.AlarmKey

case class SetSeverityTestCase(
    alarmKey: AlarmKey,
    oldSeverity: AlarmSeverity,
    newSeverity: AlarmSeverity,
    expectedLatchedSeverity: AlarmSeverity,
) {
  override def toString: String = alarmKey.name
}
