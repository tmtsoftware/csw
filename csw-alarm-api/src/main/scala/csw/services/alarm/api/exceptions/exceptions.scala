package csw.services.alarm.api.exceptions

import csw.services.alarm.api.models.AlarmSeverity.Okay
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.{FullAlarmSeverity, Key}

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
 * Represents the exception thrown when attempted to reset an alarm which is not having it's current severity as Okay
 *
 * @param key alarm for which the reset operation was attempted
 * @param currentSeverity severity currently set for the alarm which is other than Okay
 */
case class ResetOperationNotAllowed(key: Key, currentSeverity: FullAlarmSeverity)
    extends RuntimeException(
      s"Failed to reset alarm for key:[${key.value}], alarms can only be reset when severity:[${Okay.name}], but current severity is:[${currentSeverity.name}]."
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
 * Represents exception thrown when getting aggregated severity or health with no alarms are active
 *
 * @param key for which all the alarms are inactive
 */
case class InactiveAlarmException(key: Key) extends RuntimeException(s"alarms for the given key '${key.value}' are inactive")
