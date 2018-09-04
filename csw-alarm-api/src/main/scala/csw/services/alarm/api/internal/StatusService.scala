package csw.services.alarm.api.internal
import akka.Done
import csw.services.alarm.api.models.{AlarmSeverity, AlarmStatus}
import csw.services.alarm.api.models.Key.AlarmKey

import scala.concurrent.Future

private[alarm] trait StatusService {

  /**
   * To fetch status of specific alarm
   *
   * @param key represents a unique alarm in alarm store
   * @return a future which completes with status fetched from alarm store or fails with
   *         [[csw.services.alarm.api.exceptions.KeyNotFoundException]]
   */
  def getStatus(key: AlarmKey): Future[AlarmStatus]

  /**
   * To set `acknowledgement status` which is field of status to `acknowledged`
   *
   * @param key represents a unique alarm in alarm store
   * @return a future which completes when acknowledgement status of given key is set to acknowledged successfully or fails
   *         with [[csw.services.alarm.api.exceptions.KeyNotFoundException]]
   */
  def acknowledge(key: AlarmKey): Future[Done]

  /**
   * To set status of the given alarm to default state
   *
   * @note reset will set `acknowledgement status` to `acknowledged`, `latched severity` to `current severity` and other fields of
   * status remains the `same`
   * @param key represents a unique alarm in alarm store
   * @return a future which completes when status of given key is reset successfully in the alarm store or fails with
   *         [[csw.services.alarm.api.exceptions.KeyNotFoundException]]
   */
  def reset(key: AlarmKey): Future[Done]

  /**
   * To set shelve status of the given alarm to shelved
   *
   * @note shelve status is set to shelved which will `expire` after a certain time which can be configured from the configuration.
   * After timeout shelve status will be unshelved.
   * @param key represents a unique alarm in alarm store
   * @return a future which completes when shelved status of given key set to `shelved` successfully or fails with
   *         [[csw.services.alarm.api.exceptions.KeyNotFoundException]]
   */
  def shelve(key: AlarmKey): Future[Done]

  /**
   * To set shelve status of the given alarm to unshelved
   *
   * @param key represents a unique alarm in alarm store
   * @return a future which completes when shelved status of given key set to `unshelved` successfully or fails with
   *         [[csw.services.alarm.api.exceptions.KeyNotFoundException]]
   */
  def unshelve(key: AlarmKey): Future[Done]

  private[alarm] def unacknowledge(key: AlarmKey): Future[Done]
  private[alarm] def setStatus(alarmKey: AlarmKey, alarmStatus: AlarmStatus): Future[Done]
  private[alarm] def setStatus(statusMap: Map[AlarmKey, AlarmStatus]): Future[Done]
  private[alarm] def clearAllStatus(): Future[Done]
  private[alarm] def updateStatusForSeverity(key: AlarmKey, severity: AlarmSeverity): Future[Done]
}
