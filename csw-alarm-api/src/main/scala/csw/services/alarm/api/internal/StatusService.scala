package csw.services.alarm.api.internal
import csw.services.alarm.api.models.{AlarmStatus, FullAlarmSeverity}
import csw.services.alarm.api.models.Key.AlarmKey

import scala.concurrent.Future

private[alarm] trait StatusService {
  def getStatus(key: AlarmKey): Future[AlarmStatus]
  def acknowledge(key: AlarmKey): Future[Unit]
  def reset(key: AlarmKey): Future[Unit]
  def shelve(key: AlarmKey): Future[Unit]
  def unshelve(key: AlarmKey): Future[Unit]

  private[alarm] def unacknowledge(key: AlarmKey): Future[Unit]
  private[alarm] def updateStatusForSeverity(key: AlarmKey, severity: FullAlarmSeverity): Future[Unit]
}
