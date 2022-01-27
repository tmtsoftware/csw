package csw.alarm.client.internal

import csw.alarm.api.internal._
import csw.alarm.models._
import csw.time.core.models.UTCTime
import io.bullet.borer.{Codec, Json}
import romaine.codec.RomaineCodec
import romaine.codec.RomaineCodec.stringCodec

private[client] object AlarmRomaineCodec extends AlarmCodecs {
  def viaJsonCodec[A: Codec]: RomaineCodec[A] =
    stringCodec.bimap[A](
      x => Json.encode(x).toUtf8String,
      x => Json.decode(x.getBytes()).to[A].value
    )

  // Key Codecs
  implicit val metadataKeyRomaineCodec: RomaineCodec[MetadataKey]         = stringCodec.bimap(_.value, MetadataKey.apply)
  implicit val ackStatusKeyRomaineCodec: RomaineCodec[AckStatusKey]       = stringCodec.bimap(_.value, AckStatusKey.apply)
  implicit val alarmTimeKeyRomaineCodec: RomaineCodec[AlarmTimeKey]       = stringCodec.bimap(_.value, AlarmTimeKey.apply)
  implicit val shelveStatusKeyRomaineCodec: RomaineCodec[ShelveStatusKey] = stringCodec.bimap(_.value, ShelveStatusKey.apply)
  implicit val latchedSeverityKeyRomaineCodec: RomaineCodec[LatchedSeverityKey] =
    stringCodec.bimap(_.value, LatchedSeverityKey.apply)
  implicit val severityKeyRomaineCodec: RomaineCodec[SeverityKey]  = stringCodec.bimap(_.value, SeverityKey.apply)
  implicit val initializingKeyCodec: RomaineCodec[InitializingKey] = stringCodec.bimap(_.value, InitializingKey.apply)

  // value codecs
  implicit val metadataRomaineCodec: RomaineCodec[AlarmMetadata] = viaJsonCodec
  implicit val alarmTimeRomaineCodec: RomaineCodec[UTCTime]      = viaJsonCodec
  implicit val initializingCodec: RomaineCodec[Boolean]          = stringCodec.bimap(_.toString, _.toBoolean)
}
