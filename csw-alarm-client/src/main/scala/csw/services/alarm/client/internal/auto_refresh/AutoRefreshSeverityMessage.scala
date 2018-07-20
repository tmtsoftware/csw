package csw.services.alarm.client.internal.auto_refresh

import csw.services.alarm.api.models.{AlarmKey, AlarmSeverity}

sealed trait AutoRefreshSeverityMessage {
  def key: AlarmKey
  def severity: AlarmSeverity
}

object AutoRefreshSeverityMessage {
  case class SetSeverityAndAutoRefresh(key: AlarmKey, severity: AlarmSeverity) extends AutoRefreshSeverityMessage
  case class RefreshSeverity(key: AlarmKey, severity: AlarmSeverity)           extends AutoRefreshSeverityMessage
}
