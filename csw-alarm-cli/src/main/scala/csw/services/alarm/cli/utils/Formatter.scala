package csw.services.alarm.cli.utils

import csw.services.alarm.api.models.{AlarmMetadata, AlarmStatus, FullAlarmSeverity}

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
      s"Alarm Time: ${alarmTime.map(_.time.toString).getOrElse("")}"
    ).mkString(Newline)
  }

  def formatSeverity(severity: FullAlarmSeverity): String = s"Current Alarm Severity: ${severity.toString}"

}
