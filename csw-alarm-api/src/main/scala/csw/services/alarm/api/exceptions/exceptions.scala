package csw.services.alarm.api.exceptions

import csw.services.alarm.api.models.AlarmSeverity.Okay
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.{AlarmSeverity, Key}

case class InvalidSeverityException(key: Key, supportedSeverities: Set[AlarmSeverity], invalidSeverity: AlarmSeverity)
    extends RuntimeException(
      s"Attempt to set invalid severity [${invalidSeverity.name}] for alarm [${key.value}]. " +
      s"Supported severities for this alarm are [${supportedSeverities.mkString(",")}]"
    )

case class ResetOperationNotAllowed(key: Key, currentSeverity: AlarmSeverity)
    extends RuntimeException(
      s"Failed to reset alarm for key:[${key.value}], alarms can only be reset when severity:[${Okay.name}], but current severity is:[${currentSeverity.name}]."
    )

case class ConfigParseException(reasons: List[String]) extends RuntimeException(reasons.mkString("[", "\n", "]"))

case class KeyNotFoundException private (message: String) extends RuntimeException(message)

object KeyNotFoundException {

  def apply(key: AlarmKey): KeyNotFoundException = new KeyNotFoundException(s"Key: [${key.value}] not found in Alarm Store.")
  def apply(key: Key): KeyNotFoundException =
    new KeyNotFoundException(s"Key: [${key.value}] does not match any key in Alarm store.")
}

case class InactiveAlarmException(key: Key) extends RuntimeException(s"alarms for the given key '$key' are inactive")
