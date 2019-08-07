package csw.params.core.formats

import enumeratum.{Enum, EnumEntry}
import io.bullet.borer.{Cbor, Codec, Decoder, Encoder}

object CodecHelpers {
  def bimap[From: Encoder: Decoder, To](to: From => To, from: To => From): Codec[To] = Codec(
    implicitly[Encoder[From]].contramap(from),
    implicitly[Decoder[From]].map(to)
  )

  def enumCodec[T <: EnumEntry: Enum]: Codec[T] = bimap[String, T](implicitly[Enum[T]].withNameInsensitive, _.entryName)
}
