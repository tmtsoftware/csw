package csw.alarm.client.internal.auto_refresh

import csw.alarm.api.models.AlarmSeverity
import csw.alarm.api.models.Key.AlarmKey

trait Refreshable {
  def refreshSeverity(key: AlarmKey, severity: AlarmSeverity): Unit
}
