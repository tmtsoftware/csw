package csw.params.core.formats

import csw.params.core.models.Prefix
import enumeratum.{Enum, EnumEntry}
import io.bullet.borer.Codec

trait CommonCodecs {
  implicit lazy val prefixCodec: Codec[Prefix] = Codec.bimap[String, Prefix](_.toString, Prefix(_))
  implicit def enumCodec[T <: EnumEntry: Enum]: Codec[T] = Codec.bimap[String, T](
    _.entryName,
    implicitly[Enum[T]].withNameInsensitive
  )
}
