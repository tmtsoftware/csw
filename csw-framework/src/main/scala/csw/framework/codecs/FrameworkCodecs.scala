package csw.framework.codecs

import csw.command.client.models.framework.ComponentInfo
import csw.framework.models.{ContainerBootstrapInfo, ContainerInfo, HostBootstrapInfo}
import csw.location.models.codecs.LocationCodecs
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

import scala.concurrent.duration.FiniteDuration

trait FrameworkCodecs extends LocationCodecs {
  implicit lazy val finiteDurationCodec: Codec[FiniteDuration] = Codec.bimap[String, FiniteDuration](
    _.toString(),
    _.split(" ") match {
      case Array(length, unit) => FiniteDuration(length.toLong, unit)
      case _                   => throw new RuntimeException("error.expected.duration.finite")
    }
  )
  implicit lazy val componentInfoCodec: Codec[ComponentInfo]                   = deriveCodec
  implicit lazy val containerInfoCodec: Codec[ContainerInfo]                   = deriveCodec
  implicit lazy val containerBootstrapInfoCodec: Codec[ContainerBootstrapInfo] = deriveCodec
  implicit lazy val hostBootstrapInfoCodec: Codec[HostBootstrapInfo]           = deriveCodec
}
