package csw.services.alarm.client.internal.helpers

import csw.services.alarm.api.models.AlarmSeverity
import csw.services.alarm.api.models.Key.AlarmKey

case class SetSeverityTestCase(oldSeverity: AlarmSeverity,
                               newSeverity: AlarmSeverity,
                               outcome: AlarmSeverity,
                               alarmKey: AlarmKey) {
  override def toString: String = alarmKey.name
}
