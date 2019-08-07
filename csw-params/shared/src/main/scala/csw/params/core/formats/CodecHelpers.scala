package csw.params.core.formats

import enumeratum.{Enum, EnumEntry}
import io.bullet.borer.Codec

object CodecHelpers {
  def enumCodec[T <: EnumEntry: Enum]: Codec[T] = Codec.bimap[String, T](_.entryName, implicitly[Enum[T]].withNameInsensitive)
}
