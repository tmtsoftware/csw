package csw.services.alarm.client.internal

import csw.services.alarm.api.internal._
import romaine.codec.RomaineStringCodec
import upickle.default._

object AlarmCodec extends AlarmRW {
  implicit def viaJsonCodec[A: ReadWriter]: RomaineStringCodec[A] = RomaineStringCodec.codec(
    x ⇒ write(x),
    x ⇒ read[A](x)
  )

  implicit val metadataRomaineCodec: RomaineStringCodec[MetadataKey] = RomaineStringCodec.codec(_.value, MetadataKey.apply)
  implicit val statusRomaineCodec: RomaineStringCodec[StatusKey]     = RomaineStringCodec.codec(_.value, StatusKey.apply)
  implicit val severityRomaineCodec: RomaineStringCodec[SeverityKey] = RomaineStringCodec.codec(_.value, SeverityKey.apply)
}
