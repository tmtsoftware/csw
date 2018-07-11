package csw.services.alarm.api.scaladsl

import csw.services.alarm.api.models.{AlarmKey, AlarmSeverity}

import scala.concurrent.Future

trait AlarmAdminService extends AlarmService {
  def getSeverity(key: AlarmKey): Future[AlarmSeverity]
}
