package csw.prefix.codecs

import java.time.Instant

import csw.prefix.Prefix
import enumeratum.{Enum, EnumEntry}
import io.bullet.borer.derivation.MapBasedCodecs.{deriveDecoder, deriveEncoder}
import io.bullet.borer.{Codec, Decoder, Encoder}

trait CommonCodecs {
  implicit lazy val prefixCodec: Codec[Prefix] = Codec.bimap[String, Prefix](_.toString, Prefix(_))

  case class Timestamp(seconds: Long, nanos: Long)
  implicit lazy val instantEnc: Encoder[Instant] = Encoder.targetSpecific(
    cbor = deriveEncoder[Timestamp].contramap(instant => Timestamp(instant.getEpochSecond, instant.getNano)),
    json = Encoder.forString.contramap(_.toString)
  )

  implicit lazy val instantDec: Decoder[Instant] = Decoder.targetSpecific(
    cbor = deriveDecoder[Timestamp].map(ts => Instant.ofEpochSecond(ts.seconds, ts.nanos)),
    json = Decoder.forString.map(Instant.parse)
  )

  implicit def enumCodec[T <: EnumEntry: Enum]: Codec[T] = Codec.bimap[String, T](
    _.entryName,
    implicitly[Enum[T]].withNameInsensitive
  )
}
