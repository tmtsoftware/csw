package csw.alarm.client.internal

import csw.alarm.api.internal._
import csw.alarm.api.models._
import csw.time.core.models.UTCTime
import play.api.libs.json.{Format, Json}
import romaine.codec.RomaineByteCodec
import romaine.codec.RomaineByteCodec.stringRomaineCodec

private[client] object AlarmCodec extends AlarmJsonSupport {
  def viaJsonCodec[A: Format]: RomaineByteCodec[A] = stringRomaineCodec.bimap[A](
    x => Json.toJson(x).toString(),
    x => Json.parse(x).as[A]
  )

  //Key Codecs
  implicit val metadataKeyRomaineCodec: RomaineByteCodec[MetadataKey]   = stringRomaineCodec.bimap(_.value, MetadataKey.apply)
  implicit val ackStatusKeyRomaineCodec: RomaineByteCodec[AckStatusKey] = stringRomaineCodec.bimap(_.value, AckStatusKey.apply)
  implicit val alarmTimeKeyRomaineCodec: RomaineByteCodec[AlarmTimeKey] = stringRomaineCodec.bimap(_.value, AlarmTimeKey.apply)
  implicit val shelveStatusKeyRomaineCodec: RomaineByteCodec[ShelveStatusKey] =
    stringRomaineCodec.bimap(_.value, ShelveStatusKey.apply)
  implicit val latchedSeverityKeyRomaineCodec: RomaineByteCodec[LatchedSeverityKey] =
    stringRomaineCodec.bimap(_.value, LatchedSeverityKey.apply)
  implicit val severityKeyRomaineCodec: RomaineByteCodec[SeverityKey]  = stringRomaineCodec.bimap(_.value, SeverityKey.apply)
  implicit val initializingKeyCodec: RomaineByteCodec[InitializingKey] = stringRomaineCodec.bimap(_.value, InitializingKey.apply)

  //value codecs
  implicit val metadataRomaineCodec: RomaineByteCodec[AlarmMetadata] = viaJsonCodec
  implicit val alarmTimeRomaineCodec: RomaineByteCodec[UTCTime]      = viaJsonCodec
  implicit val initializingCodec: RomaineByteCodec[Boolean]          = viaJsonCodec
}
