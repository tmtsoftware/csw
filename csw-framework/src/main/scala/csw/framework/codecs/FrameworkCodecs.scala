/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.codecs

import csw.command.client.models.framework.ComponentInfo
import csw.framework.models.{ContainerBootstrapInfo, ContainerInfo, HostBootstrapInfo}
import csw.location.api.codec.LocationCodecs
import io.bullet.borer.Codec
import io.bullet.borer.derivation.CompactMapBasedCodecs.deriveCodec
import io.bullet.borer.derivation.MapBasedCodecs

trait FrameworkCodecs extends LocationCodecs {
  implicit lazy val componentInfoCodec: Codec[ComponentInfo]                   = deriveCodec
  implicit lazy val containerInfoCodec: Codec[ContainerInfo]                   = deriveCodec
  implicit lazy val containerBootstrapInfoCodec: Codec[ContainerBootstrapInfo] = deriveCodec
  implicit lazy val hostBootstrapInfoCodec: Codec[HostBootstrapInfo]           = MapBasedCodecs.deriveCodec
}
