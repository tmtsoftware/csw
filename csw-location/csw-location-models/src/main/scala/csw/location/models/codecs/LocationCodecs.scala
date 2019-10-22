package csw.location.models.codecs

import java.net.URI

import csw.location.models._
import csw.params.core.formats.CommonCodecs
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs._

object LocationCodecs extends LocationCodecs
trait LocationCodecs extends CommonCodecs {
  implicit lazy val componentIdCodec: Codec[ComponentId]       = deriveCodec
  implicit lazy val connectionInfoCodec: Codec[ConnectionInfo] = deriveCodec

  implicit def connectionCodec[C <: Connection]: Codec[C] = Codec.bimap[ConnectionInfo, C](
    _.connectionInfo,
    x => Connection.from(x).asInstanceOf[C]
  )

  implicit lazy val uriCodec: Codec[URI] = Codec.bimap[String, URI](_.toString, new URI(_))

  implicit lazy val locationCodec: Codec[Location]         = deriveCodec
  implicit lazy val akkaLocationCodec: Codec[AkkaLocation] = deriveCodec
  implicit lazy val httpLocationCodec: Codec[HttpLocation] = deriveCodec
  implicit lazy val tcpLocationCodec: Codec[TcpLocation]   = deriveCodec

  implicit lazy val registrationCodec: Codec[Registration]         = deriveCodec
  implicit lazy val akkaRegistrationCodec: Codec[AkkaRegistration] = deriveCodec
  implicit lazy val tcpRegistrationCodec: Codec[TcpRegistration]   = deriveCodec
  implicit lazy val httpRegistrationCodec: Codec[HttpRegistration] = deriveCodec

  implicit lazy val trackingEventCodec: Codec[TrackingEvent]     = deriveCodec
  implicit lazy val locationUpdatedCodec: Codec[LocationUpdated] = deriveCodec
  implicit lazy val locationRemovedCodec: Codec[LocationRemoved] = deriveCodec
}
