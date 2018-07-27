package csw.services.alarm.client.internal.auto_refresh

import csw.services.alarm.api.models.AlarmSeverity
import csw.services.alarm.api.models.Key.AlarmKey

trait Refreshable {
  def refreshSeverity(key: AlarmKey, severity: AlarmSeverity): Unit
}
