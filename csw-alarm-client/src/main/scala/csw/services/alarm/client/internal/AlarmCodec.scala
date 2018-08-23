package csw.services.alarm.client.internal

import csw.services.alarm.api.internal._
import csw.services.alarm.api.models.{AlarmMetadata, AlarmStatus, FullAlarmSeverity}
import romaine.codec.RomaineStringCodec
import upickle.default._

object AlarmCodec extends AlarmRW {
  def viaJsonCodec[A: ReadWriter]: RomaineStringCodec[A] = RomaineStringCodec.codec(write[A](_), read[A](_))

  implicit val metadataRomaineCodec: RomaineStringCodec[MetadataKey] = RomaineStringCodec.codec(_.value, MetadataKey.apply)
  implicit val statusRomaineCodec: RomaineStringCodec[StatusKey]     = RomaineStringCodec.codec(_.value, StatusKey.apply)
  implicit val severityRomaineCodec: RomaineStringCodec[SeverityKey] = RomaineStringCodec.codec(_.value, SeverityKey.apply)

  implicit val metadataRomainCodec: RomaineStringCodec[AlarmMetadata]     = viaJsonCodec
  implicit val severityRomainCodec: RomaineStringCodec[FullAlarmSeverity] = viaJsonCodec
  implicit val statusRomainCodec: RomaineStringCodec[AlarmStatus]         = viaJsonCodec
}
