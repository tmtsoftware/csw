package csw.services.alarm.api.internal
import akka.Done
import csw.services.alarm.api.models.{AlarmSeverity, AlarmStatus}
import csw.services.alarm.api.models.Key.AlarmKey

import scala.concurrent.Future

private[alarm] trait StatusService {
  def getStatus(key: AlarmKey): Future[AlarmStatus]
  def acknowledge(key: AlarmKey): Future[Done]
  def reset(key: AlarmKey): Future[Done]
  def shelve(key: AlarmKey): Future[Done]
  def unshelve(key: AlarmKey): Future[Done]

  private[alarm] def unacknowledge(key: AlarmKey): Future[Done]
  private[alarm] def setStatus(alarmKey: AlarmKey, alarmStatus: AlarmStatus): Future[Done]
  private[alarm] def setStatus(statusMap: Map[AlarmKey, AlarmStatus]): Future[Done]
  private[alarm] def clearAllStatus(): Future[Done]
  private[alarm] def updateStatusForSeverity(key: AlarmKey, severity: AlarmSeverity): Future[Done]
}
