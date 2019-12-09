package csw.location.models.codecs

import java.net.URI

import com.github.ghik.silencer.silent
import csw.location.models._
import csw.prefix.codecs.CommonCodecs
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

  implicit def locationCodec[T <: Location]: Codec[T] = locationCodecValue.asInstanceOf[Codec[T]]

  lazy val locationCodecValue: Codec[Location] = {
    @silent implicit lazy val akkaLocationCodec: Codec[AkkaLocation] = deriveCodec
    @silent implicit lazy val httpLocationCodec: Codec[HttpLocation] = deriveCodec
    @silent implicit lazy val tcpLocationCodec: Codec[TcpLocation]   = deriveCodec
    deriveCodec
  }

  implicit lazy val registrationCodec: Codec[Registration]         = deriveCodec
  implicit lazy val akkaRegistrationCodec: Codec[AkkaRegistration] = deriveCodec
  implicit lazy val tcpRegistrationCodec: Codec[TcpRegistration]   = deriveCodec
  implicit lazy val httpRegistrationCodec: Codec[HttpRegistration] = deriveCodec

  implicit lazy val trackingEventCodec: Codec[TrackingEvent]     = deriveCodec
  implicit lazy val locationUpdatedCodec: Codec[LocationUpdated] = deriveCodec
  implicit lazy val locationRemovedCodec: Codec[LocationRemoved] = deriveCodec
}
