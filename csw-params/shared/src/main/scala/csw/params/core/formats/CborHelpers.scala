package csw.params.core.formats

import enumeratum.{Enum, EnumEntry}
import io.bullet.borer.{Cbor, Codec, Decoder, Encoder}

object CborHelpers {
  def bimap[From: Encoder: Decoder, To](to: From ⇒ To, from: To ⇒ From): Codec[To] = Codec(
    implicitly[Encoder[From]].contramap(from),
    implicitly[Decoder[From]].map(to)
  )

  def enumCodec[T <: EnumEntry: Enum]: Codec[T] = bimap[String, T](implicitly[Enum[T]].withNameInsensitive, _.entryName)

  def targetSpecificEnc[T](cborEnc: Encoder[T], jsonEnc: Encoder[T]): Encoder[T] = { (writer, value) ⇒
    val enc = if (writer.target == Cbor) cborEnc else jsonEnc
    enc.write(writer, value)
  }

  def targetSpecificDec[T](cborDec: Decoder[T], jsonDec: Decoder[T]): Decoder[T] = { reader ⇒
    val dec = if (reader.target == Cbor) cborDec else jsonDec
    dec.read(reader)
  }
}
