package csw.services.alarm.api.scaladsl

import csw.services.alarm.api.models.{AlarmKey, AlarmMetadata, AlarmSeverity, AlarmStatus}

import scala.concurrent.Future

trait AlarmAdminService extends AlarmService {
  def getSeverity(key: AlarmKey): Future[AlarmSeverity]
  def getMetadata(key: AlarmKey): Future[AlarmMetadata]
  def getStatus(key: AlarmKey): Future[AlarmStatus]
}
