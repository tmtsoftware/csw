package csw.services.alarm.api.exceptions

import csw.services.alarm.api.models.{AlarmKey, AlarmSeverity}

case class InvalidSeverityException(key: AlarmKey, supportedSeverities: List[AlarmSeverity], invalidSeverity: AlarmSeverity)
    extends RuntimeException(
      s"Attempt to set invalid severity [$invalidSeverity] for alarm [${key.name}]. " +
      s"Supported severities for this alarm are [${supportedSeverities.mkString(",")}]"
    )
