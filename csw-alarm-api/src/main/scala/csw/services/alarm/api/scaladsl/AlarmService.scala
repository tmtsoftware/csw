package csw.services.alarm.api.scaladsl

import csw.services.alarm.api.models.{AlarmKey, AlarmSeverity}

import scala.concurrent.Future

trait AlarmService {
  def setSeverity(key: AlarmKey, severity: AlarmSeverity, autoRefresh: Boolean): Future[Unit]
}
