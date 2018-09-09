package csw.services.alarm.api.internal

import csw.messages.extensions.Formats
import csw.messages.extensions.Formats.MappableFormat
import csw.messages.params.models.Subsystem
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
  implicit lazy val subsystemFormat: Format[Subsystem]                         = EnumJsonSupport.format
  implicit lazy val alarmSeverityFormat: Format[FullAlarmSeverity]             = EnumJsonSupport.format
  implicit lazy val acknowledgementStatusFormat: Format[AcknowledgementStatus] = EnumJsonSupport.format
  implicit lazy val activationStatusFormat: Format[ActivationStatus]           = EnumJsonSupport.format
  implicit lazy val alarmTypeFormat: Format[AlarmType]                         = EnumJsonSupport.format
  implicit lazy val shelveStatusFormat: Format[ShelveStatus]                   = EnumJsonSupport.format
  implicit lazy val alarmHealthFormat: Format[AlarmHealth]                     = EnumJsonSupport.format
}
