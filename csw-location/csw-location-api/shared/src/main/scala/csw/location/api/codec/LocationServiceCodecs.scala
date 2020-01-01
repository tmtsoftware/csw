package csw.location.api.codec

import csw.location.api.exceptions._
import csw.location.api.messages.{LocationHttpMessage, LocationWebsocketMessage}
import csw.location.models.codecs.LocationCodecs
import io.bullet.borer.Codec
import io.bullet.borer.derivation.CompactMapBasedCodecs
import msocket.api.ErrorProtocol

object LocationServiceCodecs extends LocationServiceCodecs

trait LocationServiceCodecs extends LocationCodecs {

  implicit lazy val locationHttpMessageCodecValue: Codec[LocationHttpMessage]           = CompactMapBasedCodecs.deriveAllCodecs
  implicit lazy val locationWebsocketMessageCodecValue: Codec[LocationWebsocketMessage] = CompactMapBasedCodecs.deriveAllCodecs
  implicit lazy val LocationServiceErrorCodecValue: Codec[LocationServiceError]         = CompactMapBasedCodecs.deriveAllCodecs

  implicit lazy val locationHttpMessageErrorProtocol: ErrorProtocol[LocationHttpMessage] =
    ErrorProtocol.bind[LocationHttpMessage, LocationServiceError]

  implicit lazy val locationWebsocketMessageErrorProtocol: ErrorProtocol[LocationWebsocketMessage] =
    ErrorProtocol.bind[LocationWebsocketMessage, LocationServiceError]
}
