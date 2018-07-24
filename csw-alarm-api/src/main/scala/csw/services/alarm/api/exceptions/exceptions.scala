package csw.services.alarm.api.exceptions

import csw.services.alarm.api.models.{AlarmKey, AlarmSeverity}

case class InvalidSeverityException(key: AlarmKey, supportedSeverities: List[AlarmSeverity], invalidSeverity: AlarmSeverity)
    extends RuntimeException(
      s"Attempt to set invalid severity [${invalidSeverity.name}] for alarm [${key.name}]. " +
      s"Supported severities for this alarm are [${supportedSeverities.mkString(",")}]"
    )

case class ResetOperationFailedException(key: AlarmKey, currentSeverity: AlarmSeverity)
    extends RuntimeException(
      s"Attempt to reset alarm [${key.name}] has failed because current severity is set to [${currentSeverity.name}]."
    )

case object NoAlarmsFoundException extends RuntimeException("No alarms found")

case class ConfigParseException(reasons: List[String]) extends RuntimeException(reasons.mkString("[", "\n", "]"))
