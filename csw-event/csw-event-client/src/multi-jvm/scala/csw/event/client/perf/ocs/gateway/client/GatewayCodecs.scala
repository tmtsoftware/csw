/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.client.perf.ocs.gateway.client

import csw.event.client.perf.ocs.gateway.client.GatewayMessages.{GatewayException, GatewayStreamRequest}
import csw.params.core.formats.ParamCodecs
import csw.params.events.EventKey
import io.bullet.borer.Codec
import io.bullet.borer.derivation.CompactMapBasedCodecs.deriveCodec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs
import msocket.api.ErrorProtocol

object GatewayCodecs extends GatewayCodecs

trait GatewayCodecs extends ParamCodecs {

  implicit lazy val websocketRequestCodecValue: Codec[GatewayStreamRequest] = deriveAllCodecs
  implicit lazy val gatewayExceptionCodecValue: Codec[GatewayException]     = deriveAllCodecs
  implicit lazy val WebsocketRequestErrorProtocol: ErrorProtocol[GatewayStreamRequest] =
    ErrorProtocol.bind[GatewayStreamRequest, GatewayException]
  implicit lazy val eventKeyCodec: Codec[EventKey] = deriveCodec
}
