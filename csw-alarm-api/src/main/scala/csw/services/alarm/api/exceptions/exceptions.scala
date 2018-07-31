package csw.services.alarm.api.exceptions

import csw.services.alarm.api.models.AlarmSeverity.Okay
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

case class NoAlarmsFoundException() extends RuntimeException("No alarms found")

case class ConfigParseException(reasons: List[String]) extends RuntimeException(reasons.mkString("[", "\n", "]"))

case class RedisOperationFailed(msg: String, ex: Throwable = None.orNull) extends RuntimeException(msg, ex)

case class KeyNotFoundException(key: Key) extends RuntimeException(s"Key: [${key.value}] not found in Alarm Store.")
