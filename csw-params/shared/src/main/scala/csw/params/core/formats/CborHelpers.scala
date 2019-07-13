package csw.params.core.formats

import enumeratum.{Enum, EnumEntry}
import io.bullet.borer.{Codec, Decoder, Encoder}

object CborHelpers {
  def bimap[From: Encoder: Decoder, To](to: From => To, from: To => From): Codec[To] = Codec(
    implicitly[Encoder[From]].contramap(from),
    implicitly[Decoder[From]].map(to)
  )

  def enumCodec[T <: EnumEntry: Enum]: Codec[T] = bimap[String, T](implicitly[Enum[T]].withNameInsensitive, _.entryName)
}
