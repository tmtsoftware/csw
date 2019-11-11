package csw.params.core.formats

import csw.params.core.models.{Prefix, Subsystem}
import enumeratum.{Enum, EnumEntry}
import io.bullet.borer.{Codec, Decoder, Encoder}
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

trait CommonCodecs {
  implicit lazy val subsystemEncCodec: Encoder[Subsystem] = Encoder.forString.contramap[Subsystem](_.name)
  implicit lazy val subsystemDecCodec: Decoder[Subsystem] = Decoder.forString.map(s => Subsystem.withNameInsensitive(s))

  implicit lazy val prefixCodec: Codec[Prefix] = deriveCodec

  implicit def enumCodec[T <: EnumEntry: Enum]: Codec[T] = Codec.bimap[String, T](
    _.entryName,
    implicitly[Enum[T]].withNameInsensitive
  )
}
