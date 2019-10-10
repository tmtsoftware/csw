package csw.framework.codecs

import csw.framework.models.{ContainerBootstrapInfo, ContainerInfo, HostBootstrapInfo}
import csw.params.core.formats.CommonCodecs
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

trait FrameworkCodecs extends CommonCodecs {
  implicit lazy val containerInfoCodec: Codec[ContainerInfo]                   = deriveCodec
  implicit lazy val containerBootstrapInfoCodec: Codec[ContainerBootstrapInfo] = deriveCodec
  implicit lazy val hostBootstrapInfoCodec: Codec[HostBootstrapInfo]           = deriveCodec
}
