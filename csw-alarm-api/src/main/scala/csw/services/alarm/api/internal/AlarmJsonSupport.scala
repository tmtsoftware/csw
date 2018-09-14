package csw.services.alarm.api.internal

import csw.params.extensions.Formats
import csw.params.extensions.Formats.MappableFormat
import csw.params.core.models.Subsystem
import csw.services.alarm.api.models._
import play.api.libs.json.{Format, Json}

private[alarm] trait AlarmJsonSupport {
  implicit lazy val alarmTimeFormat: Format[AlarmTime] = Formats.of[String].bimap(_.value, AlarmTime.apply)
  implicit lazy val alarmMetadataFormat: Format[AlarmMetadata] =
    Json
      .format[AlarmMetadata]
      .bimap(identity, metadata ⇒ metadata.copy(supportedSeverities = metadata.allSupportedSeverities))
  implicit val alarmMetadataSetFormat: Format[AlarmMetadataSet] = Json.format

  implicit lazy val alarmStatusFormat: Format[AlarmStatus]                     = Json.format
  implicit lazy val subsystemFormat: Format[Subsystem]                         = Formats.enumFormat
  implicit lazy val alarmSeverityFormat: Format[FullAlarmSeverity]             = Formats.enumFormat
  implicit lazy val acknowledgementStatusFormat: Format[AcknowledgementStatus] = Formats.enumFormat
  implicit lazy val activationStatusFormat: Format[ActivationStatus]           = Formats.enumFormat
  implicit lazy val alarmTypeFormat: Format[AlarmType]                         = Formats.enumFormat
  implicit lazy val shelveStatusFormat: Format[ShelveStatus]                   = Formats.enumFormat
  implicit lazy val alarmHealthFormat: Format[AlarmHealth]                     = Formats.enumFormat
}
