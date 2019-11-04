package csw.params.core.formats

import csw.params.core.models.Prefix
import enumeratum.{Enum, EnumEntry}
import io.bullet.borer.Codec

import scala.concurrent.duration.FiniteDuration

trait CommonCodecs {
  implicit lazy val prefixCodec: Codec[Prefix] = Codec.bimap[String, Prefix](_.toString, Prefix(_))
  implicit def enumCodec[T <: EnumEntry: Enum]: Codec[T] = Codec.bimap[String, T](
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
