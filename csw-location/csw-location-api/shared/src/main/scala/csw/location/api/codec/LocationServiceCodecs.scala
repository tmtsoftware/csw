/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.api.codec

import csw.location.api.exceptions.*
import csw.location.api.messages.{LocationRequest, LocationStreamRequest}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs
import msocket.api.ErrorProtocol

object LocationServiceCodecs extends LocationServiceCodecs

trait LocationServiceCodecs extends LocationCodecs {

  implicit lazy val locationHttpMessageCodec: Codec[LocationRequest]            = deriveAllCodecs
  implicit lazy val locationWebsocketMessageCodec: Codec[LocationStreamRequest] = deriveAllCodecs
  implicit lazy val LocationServiceErrorCodec: Codec[LocationServiceError]      = deriveAllCodecs

  implicit lazy val locationHttpMessageErrorProtocol: ErrorProtocol[LocationRequest] =
    ErrorProtocol.bind[LocationRequest, LocationServiceError]

  implicit lazy val locationWebsocketMessageErrorProtocol: ErrorProtocol[LocationStreamRequest] =
    ErrorProtocol.bind[LocationStreamRequest, LocationServiceError]
}
