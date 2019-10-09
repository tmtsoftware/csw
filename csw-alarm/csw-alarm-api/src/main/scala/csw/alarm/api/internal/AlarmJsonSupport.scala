package csw.alarm.api.internal

import csw.alarm.models._
import csw.params.extensions.Formats
import csw.params.extensions.Formats.MappableFormat
import enumeratum.{Enum, EnumEntry}
import play.api.libs.json.{Format, Json}

private[alarm] trait AlarmJsonSupport {
  implicit lazy val alarmMetadataFormat: Format[AlarmMetadata] =
    Json
      .format[AlarmMetadata]
      .bimap(identity, metadata => metadata.copy(supportedSeverities = metadata.allSupportedSeverities))
  implicit val alarmMetadataSetFormat: Format[AlarmMetadataSet] = Json.format

  implicit lazy val alarmStatusFormat: Format[AlarmStatus] = Json.format

  implicit def enumFormat[T <: EnumEntry: Enum]: Format[T] =
    Formats.of[String].bimap[T](_.entryName, implicitly[Enum[T]].withNameInsensitive)
}
