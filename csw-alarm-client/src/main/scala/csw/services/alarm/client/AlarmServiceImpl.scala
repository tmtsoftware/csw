package csw.services.alarm.client

import csw.services.alarm.api.models.{AlarmKey, AlarmSeverity}
import csw.services.alarm.api.scaladsl.AlarmAdminService

import scala.concurrent.Future

class AlarmServiceImpl extends AlarmAdminService {
  override def setSeverity(key: AlarmKey, severity: AlarmSeverity): Future[Unit] = ???

  override def getSeverity(key: AlarmKey): Future[AlarmSeverity] = ???
}
