package csw.framework.codecs

import csw.command.client.models.framework.ComponentInfo
import csw.framework.models.{ContainerBootstrapInfo, ContainerInfo, HostBootstrapInfo}
import csw.location.models.codecs.LocationCodecs
import csw.params.core.formats.CommonCodecs
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec
import msocket.api.codecs.BasicCodecs

trait FrameworkCodecs extends CommonCodecs with BasicCodecs with LocationCodecs {
  implicit lazy val componentInfoCodec: Codec[ComponentInfo]                   = deriveCodec
  implicit lazy val containerInfoCodec: Codec[ContainerInfo]                   = deriveCodec
  implicit lazy val containerBootstrapInfoCodec: Codec[ContainerBootstrapInfo] = deriveCodec
  implicit lazy val hostBootstrapInfoCodec: Codec[HostBootstrapInfo]           = deriveCodec
}
