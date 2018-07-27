package csw.services.alarm.api.exceptions

import csw.services.alarm.api.models.{AlarmSeverity, Key}

case class InvalidSeverityException(key: Key, supportedSeverities: Set[AlarmSeverity], invalidSeverity: AlarmSeverity)
    extends RuntimeException(
      s"Attempt to set invalid severity [${invalidSeverity.name}] for alarm [${key.value}]. " +
      s"Supported severities for this alarm are [${supportedSeverities.mkString(",")}]"
    )

case class ResetOperationFailedException(key: Key, currentSeverity: AlarmSeverity)
    extends RuntimeException(
      s"Attempt to reset alarm [${key.value}] has failed because current severity is set to [${currentSeverity.name}]."
    )

case class NoAlarmsFoundException() extends RuntimeException("No alarms found")

case class ConfigParseException(reasons: List[String]) extends RuntimeException(reasons.mkString("[", "\n", "]"))
