package csw.alarm.api.internal

import csw.alarm.models._
import csw.params.core.formats.ParamCodecs
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

private[alarm] trait AlarmCodecs extends ParamCodecs {
  implicit lazy val alarmMetadataFormat: Codec[AlarmMetadata] = deriveCodec[AlarmMetadata].bimap[AlarmMetadata](
    identity,
    metadata => metadata.copy(supportedSeverities = metadata.allSupportedSeverities)
  )

  implicit lazy val alarmMetadataSetFormat: Codec[AlarmMetadataSet] = deriveCodec
  implicit lazy val alarmStatusFormat: Codec[AlarmStatus]           = deriveCodec
}
