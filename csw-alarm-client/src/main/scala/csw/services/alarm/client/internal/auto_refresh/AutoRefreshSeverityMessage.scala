package csw.services.alarm.client.internal.auto_refresh

import csw.services.alarm.api.models.AlarmSeverity
import csw.services.alarm.api.models.Key.AlarmKey

sealed trait AutoRefreshSeverityMessage {
  def key: AlarmKey
}

object AutoRefreshSeverityMessage {
  case class SetSeverityAndAutoRefresh(key: AlarmKey, severity: AlarmSeverity) extends AutoRefreshSeverityMessage
  case class RefreshSeverity(key: AlarmKey, severity: AlarmSeverity)           extends AutoRefreshSeverityMessage
  case class CancelAutoRefresh(key: AlarmKey)                                  extends AutoRefreshSeverityMessage
}
