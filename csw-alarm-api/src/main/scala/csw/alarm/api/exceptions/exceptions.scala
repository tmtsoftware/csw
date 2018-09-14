package csw.alarm.api.exceptions

import csw.alarm.api.models.Key.AlarmKey
import csw.alarm.api.models.{FullAlarmSeverity, Key}

/**
 * Represents the exception thrown when it is attempted to set severity that is not supported by the alarm
 *
 * @param key alarm for which the severity was attempted to change
 * @param supportedSeverities list of severities supported by this alarm
 * @param invalidSeverity not supported by the alarm
 */
case class InvalidSeverityException(key: Key, supportedSeverities: Set[FullAlarmSeverity], invalidSeverity: FullAlarmSeverity)
    extends RuntimeException(
      s"Attempt to set invalid severity [${invalidSeverity.name}] for alarm [${key.value}]. " +
      s"Supported severities for this alarm are [${supportedSeverities.mkString(",")}]"
    )

/**
 *  Represents the exception thrown while parsing the alarm metadata config file for populating alarm store
 *
 * @param reasons collection of problems while parsing the config file
 */
case class ConfigParseException(reasons: List[String]) extends RuntimeException(reasons.mkString("[", "\n", "]"))

/**
 *  Represents the exception thrown while referring to an invalid key that is not present in alarm store.
 *
 * @param message represents the message with the key that is not present in the alarm store
 */
case class KeyNotFoundException private (message: String) extends RuntimeException(message)

private[alarm] object KeyNotFoundException {

  def apply(key: AlarmKey): KeyNotFoundException =
    new KeyNotFoundException(s"Key: [${key.value}] not found in Alarm Store.")

  def apply(key: Key): KeyNotFoundException =
    new KeyNotFoundException(s"Key: [${key.value}] does not match any key in Alarm store.")
}

/**
 * Represents exception thrown when getting aggregated severity or health with no actives alarms
 *
 * @param key for which all the alarms are inactive
 */
case class InactiveAlarmException(key: Key) extends RuntimeException(s"alarms for the given key '${key.value}' are inactive")
