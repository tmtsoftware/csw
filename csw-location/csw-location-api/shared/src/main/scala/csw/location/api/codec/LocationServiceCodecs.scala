package csw.location.api.codec

import csw.location.api.exceptions._
import csw.location.api.messages.{LocationRequest, LocationStreamingRequest}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs
import msocket.api.ErrorProtocol

object LocationServiceCodecs extends LocationServiceCodecs

trait LocationServiceCodecs extends LocationCodecs {

  implicit lazy val locationHttpMessageCodec: Codec[LocationRequest]               = deriveAllCodecs
  implicit lazy val locationWebsocketMessageCodec: Codec[LocationStreamingRequest] = deriveAllCodecs
  implicit lazy val LocationServiceErrorCodec: Codec[LocationServiceError]         = deriveAllCodecs

  implicit lazy val locationHttpMessageErrorProtocol: ErrorProtocol[LocationRequest] =
    ErrorProtocol.bind[LocationRequest, LocationServiceError]

  implicit lazy val locationWebsocketMessageErrorProtocol: ErrorProtocol[LocationStreamingRequest] =
    ErrorProtocol.bind[LocationStreamingRequest, LocationServiceError]
}
