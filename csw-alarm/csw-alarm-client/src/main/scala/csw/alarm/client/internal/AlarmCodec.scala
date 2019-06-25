package csw.alarm.client.internal

import csw.alarm.api.internal._
import csw.alarm.api.models._
import csw.time.core.models.UTCTime
import play.api.libs.json.{Format, Json}
import romaine.codec.RomaineByteCodec

private[client] object AlarmCodec extends AlarmJsonSupport {
  def viaJsonCodec[A: Format]: RomaineByteCodec[A] = RomaineByteCodec.viaString(
    x => Json.toJson(x).toString(),
    x => Json.parse(x).as[A]
  )

  //Key Codecs
  implicit val metadataKeyRomaineCodec: RomaineByteCodec[MetadataKey]   = RomaineByteCodec.viaString(_.value, MetadataKey.apply)
  implicit val ackStatusKeyRomaineCodec: RomaineByteCodec[AckStatusKey] = RomaineByteCodec.viaString(_.value, AckStatusKey.apply)
  implicit val alarmTimeKeyRomaineCodec: RomaineByteCodec[AlarmTimeKey] = RomaineByteCodec.viaString(_.value, AlarmTimeKey.apply)
  implicit val shelveStatusKeyRomaineCodec: RomaineByteCodec[ShelveStatusKey] =
    RomaineByteCodec.viaString(_.value, ShelveStatusKey.apply)
  implicit val latchedSeverityKeyRomaineCodec: RomaineByteCodec[LatchedSeverityKey] =
    RomaineByteCodec.viaString(_.value, LatchedSeverityKey.apply)
  implicit val severityKeyRomaineCodec: RomaineByteCodec[SeverityKey] = RomaineByteCodec.viaString(_.value, SeverityKey.apply)
  implicit val initializingKeyCodec: RomaineByteCodec[InitializingKey] =
    RomaineByteCodec.viaString(_.value, InitializingKey.apply)

  //value codecs
  implicit val metadataRomaineCodec: RomaineByteCodec[AlarmMetadata] = viaJsonCodec
  implicit val alarmTimeRomaineCodec: RomaineByteCodec[UTCTime]      = viaJsonCodec
  implicit val initializingCodec: RomaineByteCodec[Boolean]          = viaJsonCodec
}
