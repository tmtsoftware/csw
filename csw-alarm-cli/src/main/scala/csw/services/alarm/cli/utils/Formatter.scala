package csw.services.alarm.cli.utils

import csw.services.alarm.api.models.Key._
import csw.services.alarm.api.models._

object Formatter {

  val Newline = "\n"
  val Separator =
    "==============================================================================================================="

  def formatMetadataSet(metadataSet: List[AlarmMetadata]): String =
    metadataSet
      .map(metadata ⇒ formatMetadata(metadata))
      .mkString(s"$Separator$Newline", s"\n$Separator$Newline", s"$Newline$Separator")

  def formatMetadata(metadata: AlarmMetadata): String = {
    import metadata._

    List(
      s"Subsystem: $subsystem",
      s"Component: $component",
      s"Name: $name",
      s"Description: $description",
      s"Location: $location",
      s"Type: $alarmType",
      s"Supported Severities: ${allSupportedSeverities.mkString("[", ", ", "]")}",
      s"Probable Cause: $probableCause",
      s"Operator Response: $operatorResponse",
      s"AutoAcknowledgeable: $isAutoAcknowledgeable",
      s"Latchable: $isLatchable",
      s"Activation Status: $activationStatus"
    ).mkString(Newline)
  }

  def formatStatus(status: AlarmStatus): String = {
    import status._

    List(
      s"Acknowledgement Status: $acknowledgementStatus",
      s"Latch Severity: $latchedSeverity",
      s"Shelve Status: $shelveStatus",
      s"Alarm Time: ${alarmTime.value}"
    ).mkString(Newline)
  }

  def formatSeverity(key: Key, severity: FullAlarmSeverity): String        = msg(key, "Severity", severity.toString)
  def formatHealth(key: Key, health: AlarmHealth): String                  = msg(key, "Health", health.toString)
  def formatRefreshSeverity(key: Key, severity: FullAlarmSeverity): String = s"Severity for [$key] refreshed to: $severity"

  def msg(key: Key, property: String, value: String): String = key match {
    case GlobalKey                          ⇒ s"Aggregated $property of Alarm Service: $value"
    case SubsystemKey(subsystem)            ⇒ s"Aggregated $property of Subsystem [$subsystem]: $value"
    case ComponentKey(subsystem, component) ⇒ s"Aggregated $property of Component [$subsystem.$component]: $value"
    case _: AlarmKey                        ⇒ s"$property of Alarm [$key]: $value"
  }
}
