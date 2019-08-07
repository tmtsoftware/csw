package csw.logging.models.codecs

import csw.logging.models.{Level, LogMetadata}
import enumeratum.{Enum, EnumEntry}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs._

object LoggingCodecs extends LoggingCodecs
trait LoggingCodecs {
  def enumCodec[T <: EnumEntry: Enum]: Codec[T] = Codec.bimap[String, T](_.entryName, implicitly[Enum[T]].withNameInsensitive)

  implicit lazy val levelCodec: Codec[Level]             = enumCodec[Level]
  implicit lazy val logMetadataCodec: Codec[LogMetadata] = deriveCodec[LogMetadata]

}
