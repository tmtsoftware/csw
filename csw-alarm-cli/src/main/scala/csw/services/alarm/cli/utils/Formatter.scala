package csw.services.alarm.cli.utils

import csw.services.alarm.api.models.AlarmMetadata

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
      s"Supported Severities: $allSupportedSeverities",
      s"Probable Cause: $probableCause",
      s"Operator Response: $operatorResponse",
      s"AutoAcknowledgable: $isAutoAcknowledgeable",
      s"Latchable: $isLatchable",
      s"Activation Status: $activationStatus"
    ).mkString(Newline)
  }

}
