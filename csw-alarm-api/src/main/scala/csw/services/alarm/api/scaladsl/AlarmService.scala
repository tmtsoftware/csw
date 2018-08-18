package csw.services.alarm.api.scaladsl

import csw.services.alarm.api.models.AlarmSeverity
import csw.services.alarm.api.models.Key.AlarmKey

import scala.concurrent.Future

trait AlarmService {
  def setSeverity(key: AlarmKey, severity: AlarmSeverity): Future[Unit]
}
