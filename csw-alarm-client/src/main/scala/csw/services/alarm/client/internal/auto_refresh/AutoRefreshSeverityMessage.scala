package csw.services.alarm.client.internal.auto_refresh

import csw.services.alarm.api.models.FullAlarmSeverity
import csw.services.alarm.api.models.Key.AlarmKey

sealed trait AutoRefreshSeverityMessage {
  def key: AlarmKey
  def severity: FullAlarmSeverity
}

object AutoRefreshSeverityMessage {
  case class SetSeverityAndAutoRefresh(key: AlarmKey, severity: FullAlarmSeverity) extends AutoRefreshSeverityMessage
  case class RefreshSeverity(key: AlarmKey, severity: FullAlarmSeverity)           extends AutoRefreshSeverityMessage
}
