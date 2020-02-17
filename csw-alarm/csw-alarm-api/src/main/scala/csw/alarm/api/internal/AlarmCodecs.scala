package csw.alarm.api.internal

import csw.alarm.models._
import csw.prefix.codecs.CommonCodecs
import csw.time.core.models.UTCTime
import io.bullet.borer.Codec
import io.bullet.borer.derivation.CompactMapBasedCodecs.deriveCodec
import io.bullet.borer.derivation.MapBasedCodecs

private[alarm] trait AlarmCodecs extends CommonCodecs {
  implicit lazy val utcTimeCodec: Codec[UTCTime] = deriveCodec
  implicit lazy val alarmMetadataFormat: Codec[AlarmMetadata] = deriveCodec[AlarmMetadata].bimap[AlarmMetadata](
    identity,
    metadata => metadata.copy(supportedSeverities = metadata.allSupportedSeverities)
  )

  implicit lazy val alarmMetadataSetFormat: Codec[AlarmMetadataSet] = MapBasedCodecs.deriveCodec
  implicit lazy val alarmStatusFormat: Codec[AlarmStatus]           = deriveCodec
}
