package csw.services.alarm.api.internal
import akka.Done
import com.typesafe.config.Config
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.{AlarmMetadata, Key}

import scala.concurrent.Future

private[alarm] trait MetadataService {

  /**
   * To load alarm metadata to alarm store
   *
   * @note reset will remove all the alarm data available in alarm store
   * @param inputConfig to be loaded in alarm store
   * @param reset the alarm store or not
   * @return a future which completes when config is loaded successfully in alarm store
   */
  def initAlarms(inputConfig: Config, reset: Boolean = false): Future[Done]

  /**
   * To fetch alarm metadata from the alarm store for specific alarm
   *
   * @param key represents a unique alarm in alarm store
   * @return a future which completes with metadata fetched from alarm store of the given alarm key or fails with
   *          [[csw.services.alarm.api.exceptions.KeyNotFoundException]]
   */
  def getMetadata(key: AlarmKey): Future[AlarmMetadata]

  /**
   * To fetch alarm metadata from the alarm store for all alarms which belong to given subsystem or component
   *
   * @param key represents subsystem or component in alarm store
   * @return a future which completes with list of metadata fetched from alarm store for all alarms for the given key
   *         or fails with [[csw.services.alarm.api.exceptions.KeyNotFoundException]]
   */
  def getMetadata(key: Key): Future[List[AlarmMetadata]]

  private[alarm] def activate(key: AlarmKey): Future[Done]   // api only for test purpose
  private[alarm] def deactivate(key: AlarmKey): Future[Done] // api only for test purpose
}
