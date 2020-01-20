package csw.location.api.codec

import csw.location.api.exceptions._
import csw.location.api.messages.{LocationHttpMessage, LocationWebsocketMessage}
import csw.location.models.codecs.LocationCodecs
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs
import msocket.api.ErrorProtocol

object LocationServiceCodecs extends LocationServiceCodecs

trait LocationServiceCodecs extends LocationCodecs {

  implicit lazy val locationHttpMessageCodecValue: Codec[LocationHttpMessage]           = deriveAllCodecs
  implicit lazy val locationWebsocketMessageCodecValue: Codec[LocationWebsocketMessage] = deriveAllCodecs
  implicit lazy val LocationServiceErrorCodecValue: Codec[LocationServiceError]         = deriveAllCodecs

  implicit lazy val locationHttpMessageErrorProtocol: ErrorProtocol[LocationHttpMessage] =
    ErrorProtocol.bind[LocationHttpMessage, LocationServiceError]

  implicit lazy val locationWebsocketMessageErrorProtocol: ErrorProtocol[LocationWebsocketMessage] =
    ErrorProtocol.bind[LocationWebsocketMessage, LocationServiceError]
}
