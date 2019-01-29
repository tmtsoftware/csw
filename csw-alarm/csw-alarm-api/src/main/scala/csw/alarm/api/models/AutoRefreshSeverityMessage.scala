package csw.alarm.api.models

import csw.alarm.api.models.Key.AlarmKey

sealed trait AutoRefreshSeverityMessage {
  def key: AlarmKey
}

object AutoRefreshSeverityMessage {
  case class AutoRefreshSeverity(key: AlarmKey, severity: AlarmSeverity)        extends AutoRefreshSeverityMessage
  private[alarm] case class SetSeverity(key: AlarmKey, severity: AlarmSeverity) extends AutoRefreshSeverityMessage
  case class CancelAutoRefresh(key: AlarmKey)                                   extends AutoRefreshSeverityMessage
}
