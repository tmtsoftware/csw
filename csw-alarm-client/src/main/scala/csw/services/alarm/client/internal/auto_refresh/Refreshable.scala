package csw.services.alarm.client.internal.auto_refresh

import csw.services.alarm.api.models.{AlarmKey, AlarmSeverity}

trait Refreshable {
  def refreshSeverity(key: AlarmKey, severity: AlarmSeverity): Unit
}
