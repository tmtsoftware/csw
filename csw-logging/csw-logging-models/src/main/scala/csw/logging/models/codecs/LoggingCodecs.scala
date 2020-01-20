package csw.logging.models.codecs

import csw.logging.models.{Level, LogMetadata}
import enumeratum.{Enum, EnumEntry}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.CompactMapBasedCodecs.deriveCodec

object LoggingCodecs extends LoggingCodecs
trait LoggingCodecs {
  //do not name this to enumCodec as it then conflicts with an implicit value with the same name
  def explicitEnumCodec[T <: EnumEntry: Enum]: Codec[T] = Codec.bimap[String, T](
    _.entryName,
    implicitly[Enum[T]].withNameInsensitive
  )

  implicit lazy val levelCodec: Codec[Level]             = explicitEnumCodec
  implicit lazy val logMetadataCodec: Codec[LogMetadata] = deriveCodec
}
