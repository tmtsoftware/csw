package csw.services.alarm.client.internal.auto_refresh

import csw.services.alarm.api.models.FullAlarmSeverity
import csw.services.alarm.api.models.Key.AlarmKey

trait Refreshable {
  def refreshSeverity(key: AlarmKey, severity: FullAlarmSeverity): Unit
}
