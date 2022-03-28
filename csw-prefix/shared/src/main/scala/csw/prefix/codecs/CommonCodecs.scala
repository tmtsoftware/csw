/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.prefix.codecs

import java.time.Instant

import csw.prefix.models.Prefix
import enumeratum.{Enum, EnumEntry}
import io.bullet.borer.derivation.CompactMapBasedCodecs.deriveEncoder
import io.bullet.borer.derivation.CompactMapBasedCodecs.deriveDecoder
import io.bullet.borer.{AdtEncodingStrategy, Codec, Decoder, Encoder}

import scala.concurrent.duration.FiniteDuration

object CommonCodecs extends CommonCodecs
trait CommonCodecs {
  implicit val flatAdtEncoding: AdtEncodingStrategy = AdtEncodingStrategy.flat()

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

  implicit def enumCodec[T <: EnumEntry: Enum]: Codec[T] =
    Codec.bimap[String, T](
      _.entryName,
      implicitly[Enum[T]].withNameInsensitive
    )

  implicit lazy val finiteDurationCodec: Codec[FiniteDuration] = Codec.bimap[String, FiniteDuration](
    _.toString(),
    _.split(" ") match {
      case Array(length, unit) => FiniteDuration(length.toLong, unit)
      case _                   => throw new RuntimeException("error.expected.duration.finite")
    }
  )
}
