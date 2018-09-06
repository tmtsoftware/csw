package csw.services.alarm.api.internal
import akka.Done
import com.typesafe.config.Config
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.{AlarmMetadata, Key}

import scala.concurrent.Future

private[alarm] trait MetadataService {

  /**
   * Loads data for all alarms in alarm store i.e. metadata of alarms for e.g. subsystem, component, name, etc. and status of
   * alarms for e.g. acknowledgement status, latch status, etc.
   *
   * @note severity of the alarm is not loaded in store and is by default inferred as Disconnected until component starts
   *       updating severity
   * @see [[csw.services.alarm.api.models.AlarmMetadata]],
   *     [[csw.services.alarm.api.models.AlarmStatus]]
   * @param inputConfig represents the data for all alarms to be loaded in alarm store
   * @param reset the alarm store before loading the data
   * @return a future which completes when data is loaded successfully in alarm store or fails with
   *         [[csw.services.alarm.api.exceptions.ConfigParseException]]
   */
  def initAlarms(inputConfig: Config, reset: Boolean = false): Future[Done]

  /**
   * Fetches the metadata for the given alarm
   *
   * @param alarmKey represents a unique alarm in alarm store
   * @return a future which completes with metadata or fails with [[csw.services.alarm.api.exceptions.KeyNotFoundException]]
   */
  def getMetadata(alarmKey: AlarmKey): Future[AlarmMetadata]

  /**
   * Fetches the metadata of all alarms which belong to the given component/subsystem/system
   *
   * @param key represents component, subsystem or system
   * @return a future which completes with the list of metadata for the given key or fails with
   *         [[csw.services.alarm.api.exceptions.KeyNotFoundException]]
   */
  def getMetadata(key: Key): Future[List[AlarmMetadata]]

  private[alarm] def activate(key: AlarmKey): Future[Done]   // api only for test purpose
  private[alarm] def deactivate(key: AlarmKey): Future[Done] // api only for test purpose
}
