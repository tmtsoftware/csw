package csw.alarm.client.internal

import csw.alarm.api.internal._
import csw.alarm.api.models._
import csw.time.core.models.UTCTime
import play.api.libs.json.{Format, Json}
import romaine.codec.RomaineStringCodec

object AlarmCodec extends AlarmJsonSupport {
  def viaJsonCodec[A: Format]: RomaineStringCodec[A] = RomaineStringCodec.codec(
    x => Json.toJson(x).toString(),
    x => Json.parse(x).as[A]
  )

  //Key Codecs
  implicit val metadataKeyRomaineCodec: RomaineStringCodec[MetadataKey]   = RomaineStringCodec.codec(_.value, MetadataKey.apply)
  implicit val ackStatusKeyRomaineCodec: RomaineStringCodec[AckStatusKey] = RomaineStringCodec.codec(_.value, AckStatusKey.apply)
  implicit val alarmTimeKeyRomaineCodec: RomaineStringCodec[AlarmTimeKey] = RomaineStringCodec.codec(_.value, AlarmTimeKey.apply)
  implicit val shelveStatusKeyRomaineCodec: RomaineStringCodec[ShelveStatusKey] =
    RomaineStringCodec.codec(_.value, ShelveStatusKey.apply)
  implicit val latchedSeverityKeyRomaineCodec: RomaineStringCodec[LatchedSeverityKey] =
    RomaineStringCodec.codec(_.value, LatchedSeverityKey.apply)
  implicit val severityKeyRomaineCodec: RomaineStringCodec[SeverityKey] = RomaineStringCodec.codec(_.value, SeverityKey.apply)

  //value codecs
  implicit val metadataRomaineCodec: RomaineStringCodec[AlarmMetadata]     = viaJsonCodec
  implicit val severityRomaineCodec: RomaineStringCodec[FullAlarmSeverity] = viaJsonCodec
  implicit val alarmTimeRomaineCodec: RomaineStringCodec[UTCTime]          = viaJsonCodec
  implicit val shelveStatusRomaineCodec: RomaineStringCodec[ShelveStatus]  = viaJsonCodec
  implicit val ackStatusCodec: RomaineStringCodec[AcknowledgementStatus]   = viaJsonCodec
}
