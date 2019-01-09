package csw.alarm.api.internal

import csw.alarm.api.models._
import csw.params.core.models.Subsystem
import csw.params.extensions.Formats
import csw.params.extensions.Formats.MappableFormat
import play.api.libs.json.{Format, Json}

private[alarm] trait AlarmJsonSupport {
//  implicit lazy val alarmTimeFormat: Format[UTCTime] = Formats.of[String].bimap(_.stringify, UTCTime.apply)
  implicit lazy val alarmMetadataFormat: Format[AlarmMetadata] =
    Json
      .format[AlarmMetadata]
      .bimap(identity, metadata ⇒ metadata.copy(supportedSeverities = metadata.allSupportedSeverities))
  implicit val alarmMetadataSetFormat: Format[AlarmMetadataSet] = Json.format

  implicit lazy val alarmStatusFormat: Format[AlarmStatus] = Json.format

  implicit lazy val subsystemFormat: Format[Subsystem]                         = Formats.enumFormat
  implicit lazy val alarmSeverityFormat: Format[FullAlarmSeverity]             = Formats.enumFormat
  implicit lazy val acknowledgementStatusFormat: Format[AcknowledgementStatus] = Formats.enumFormat
  implicit lazy val activationStatusFormat: Format[ActivationStatus]           = Formats.enumFormat
  implicit lazy val alarmTypeFormat: Format[AlarmType]                         = Formats.enumFormat
  implicit lazy val shelveStatusFormat: Format[ShelveStatus]                   = Formats.enumFormat
  implicit lazy val alarmHealthFormat: Format[AlarmHealth]                     = Formats.enumFormat
}
