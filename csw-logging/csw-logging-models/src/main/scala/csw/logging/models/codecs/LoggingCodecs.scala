package csw.logging.models.codecs

import csw.logging.models.{Level, LogMetadata}
import enumeratum.{Enum, EnumEntry}
import io.bullet.borer.derivation.MapBasedCodecs._
import io.bullet.borer.{Codec, Decoder, Encoder}

object LoggingCodecs extends LoggingCodecs
trait LoggingCodecs {

  def bimap[From: Encoder: Decoder, To](to: From => To, from: To => From): Codec[To] = Codec(
    implicitly[Encoder[From]].contramap(from),
    implicitly[Decoder[From]].map(to)
  )

  def enumCodec[T <: EnumEntry: Enum]: Codec[T] = bimap[String, T](implicitly[Enum[T]].withNameInsensitive, _.entryName)

  implicit lazy val levelCodec: Codec[Level]             = enumCodec[Level]
  implicit lazy val logMetadataCodec: Codec[LogMetadata] = deriveCodec[LogMetadata]

}
