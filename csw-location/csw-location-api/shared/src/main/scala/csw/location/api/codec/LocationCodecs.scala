package csw.location.api.codec

import java.net.URI

import csw.location.api.models._
import csw.prefix.codecs.CommonCodecs
import io.bullet.borer.Codec
import io.bullet.borer.derivation.CompactMapBasedCodecs.deriveCodec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs

import scala.annotation.nowarn

object LocationCodecs extends LocationCodecs
trait LocationCodecs extends LocationCodecsBase {
  implicit def locationCodec[T <: Location]: Codec[T] = locationCodecValue.asInstanceOf[Codec[T]]
}
trait LocationCodecsBase extends CommonCodecs {
  implicit lazy val componentIdCodec: Codec[ComponentId]       = deriveCodec
  implicit lazy val connectionInfoCodec: Codec[ConnectionInfo] = deriveCodec

  implicit def connectionCodec[C <: Connection]: Codec[C] =
    Codec.bimap[ConnectionInfo, C](
      _.connectionInfo,
      x => Connection.from(x).asInstanceOf[C]
    )

  implicit lazy val uriCodec: Codec[URI] = Codec.bimap[String, URI](_.toString, new URI(_))

  implicit lazy val metadataCodec: Codec[Metadata]         = deriveCodec
  protected lazy val locationCodecValue: Codec[Location]   = deriveAllCodecs
  implicit lazy val registrationCodec: Codec[Registration] = deriveAllCodecs
  implicit lazy val networkType: Codec[NetworkType]        = deriveAllCodecs

  implicit lazy val trackingEventCodec: Codec[TrackingEvent] = {
    @nowarn implicit lazy val locationCodec: Codec[Location] = locationCodecValue
    deriveAllCodecs
  }
}
