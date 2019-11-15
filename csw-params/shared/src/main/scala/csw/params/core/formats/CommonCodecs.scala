package csw.params.core.formats

import csw.params.core.models.Prefix
import enumeratum.{Enum, EnumEntry}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

trait CommonCodecs {
  implicit lazy val prefixCodec: Codec[Prefix] = deriveCodec
  implicit def enumCodec[T <: EnumEntry: Enum]: Codec[T] = Codec.bimap[String, T](
    _.entryName,
    implicitly[Enum[T]].withNameInsensitive
  )
}
