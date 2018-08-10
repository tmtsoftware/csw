package csw.services.alarm.api.scaladsl
import csw.services.alarm.api.models.AlarmStatus
import csw.services.alarm.api.models.Key.AlarmKey

import scala.concurrent.Future

trait StatusService {
  def getStatus(key: AlarmKey): Future[AlarmStatus]
  def acknowledge(key: AlarmKey): Future[Unit]
  def reset(key: AlarmKey): Future[Unit]
  def shelve(key: AlarmKey): Future[Unit]
  def unShelve(key: AlarmKey): Future[Unit]

  private[alarm] def unAcknowledge(key: AlarmKey): Future[Unit]
}
