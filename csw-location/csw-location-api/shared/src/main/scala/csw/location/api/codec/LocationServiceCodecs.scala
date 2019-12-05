package csw.location.api.codec

import com.github.ghik.silencer.silent
import csw.location.api.exceptions._
import csw.location.api.messages.LocationHttpMessage._
import csw.location.api.messages.LocationWebsocketMessage.Track
import csw.location.api.messages.{LocationHttpMessage, LocationWebsocketMessage}
import csw.location.models.codecs.LocationCodecs
import io.bullet.borer.Codec
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveUnaryCodec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec
import msocket.api.ErrorType

trait LocationServiceCodecs extends LocationCodecs {

  implicit def locationHttpMessageCodec[T <: LocationHttpMessage]: Codec[LocationHttpMessage] =
    locationHttpMessageCodecValue.asInstanceOf[Codec[LocationHttpMessage]]

  lazy val locationHttpMessageCodecValue: Codec[LocationHttpMessage] = {
    @silent implicit lazy val RegisterCodec: Codec[Register]                         = deriveUnaryCodec
    @silent implicit lazy val UnregisterCodec: Codec[Unregister]                     = deriveUnaryCodec
    @silent implicit lazy val UnregisterAllCodec: Codec[UnregisterAll.type]          = deriveCodec
    @silent implicit def FindCodec: Codec[Find]                                      = deriveUnaryCodec
    @silent implicit def ResolveCodec: Codec[Resolve]                                = deriveCodec
    @silent implicit lazy val ListCodec: Codec[ListEntries.type]                     = deriveCodec
    @silent implicit lazy val ListByComponentTypeCodec: Codec[ListByComponentType]   = deriveUnaryCodec
    @silent implicit lazy val ListByHostnameCodec: Codec[ListByHostname]             = deriveUnaryCodec
    @silent implicit lazy val ListByConnectionTypeCodec: Codec[ListByConnectionType] = deriveUnaryCodec
    @silent implicit lazy val ListByPrefixCodec: Codec[ListByPrefix]                 = deriveUnaryCodec
    deriveCodec
  }

  implicit def locationWebsocketMessageCodec[T <: LocationWebsocketMessage]: Codec[T] =
    locationWebsocketMessageCodecValue.asInstanceOf[Codec[T]]

  lazy val locationWebsocketMessageCodecValue: Codec[LocationWebsocketMessage] = {
    @silent implicit lazy val TrackCoec: Codec[Track] = deriveUnaryCodec
    deriveCodec
  }

  implicit def LocationServiceErrorrCodec[T <: LocationServiceError]: Codec[T] =
    LocationServiceErrorCodecValue.asInstanceOf[Codec[T]]

  lazy val LocationServiceErrorCodecValue: Codec[LocationServiceError] = {
    @silent implicit lazy val RegistrationFailedCodec: Codec[RegistrationFailed]               = deriveUnaryCodec
    @silent implicit lazy val OtherLocationIsRegisteredCodec: Codec[OtherLocationIsRegistered] = deriveUnaryCodec
    @silent implicit lazy val UnregistrationFailedCodec: Codec[UnregistrationFailed]           = deriveUnaryCodec
    @silent implicit lazy val RegistrationListingFailedCodec: Codec[RegistrationListingFailed] = deriveCodec
    deriveCodec
  }

  implicit lazy val locationHttpMessageErrorTye: ErrorType[LocationHttpMessage] =
    ErrorType.bind[LocationHttpMessage, LocationServiceError]

  implicit lazy val locationWebsocketMessageErrorTye: ErrorType[LocationWebsocketMessage] =
    ErrorType.bind[LocationWebsocketMessage, LocationServiceError]
}
