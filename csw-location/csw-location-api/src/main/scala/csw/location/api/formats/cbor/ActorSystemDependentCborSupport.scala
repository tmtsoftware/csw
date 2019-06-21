package csw.location.api.formats.cbor

import java.net.URI

import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.serialization.{Serialization, SerializationExtension}
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.location.api.models.{ComponentId, ComponentType, Connection, ConnectionInfo, ConnectionType, Location, TrackingEvent}
import csw.params.core.formats.{CborCommonSupport, CborHelpers}
import io.bullet.borer._
import io.bullet.borer.derivation.MapBasedCodecs._

trait ActorSystemDependentCborSupport extends CborCommonSupport {
  implicit def actorSystem: ActorSystem[_]

  implicit lazy val connectionTypeCodec: Codec[ConnectionType] = CborHelpers.enumCodec[ConnectionType]
  implicit lazy val componentTypeCodec: Codec[ComponentType]   = CborHelpers.enumCodec[ComponentType]
  implicit lazy val componentIdCodec: Codec[ComponentId]       = deriveCodec[ComponentId]
  implicit lazy val componentInfoCodec: Codec[ConnectionInfo]  = deriveCodec[ConnectionInfo]

  implicit lazy val connectionCodec: Codec[Connection] =
    CborHelpers.bimap[ConnectionInfo, Connection](Connection.from, _.connectionInfo)

  implicit lazy val akkaConnectionCodec: Codec[AkkaConnection] = deriveCodec[AkkaConnection]
  implicit lazy val httpConnectionCodec: Codec[HttpConnection] = deriveCodec[HttpConnection]
  implicit lazy val tcpConnectionCodec: Codec[TcpConnection]   = deriveCodec[TcpConnection]

  implicit lazy val uriCodec: Codec[URI] = CborHelpers.bimap[String, URI](new URI(_), _.toString)

  implicit def actorRefCodec[T]: Codec[ActorRef[T]] =
    CborHelpers.bimap[String, ActorRef[T]](
      path => {
        val provider = SerializationExtension(actorSystem.toUntyped).system.provider
        provider.resolveActorRef(path)
      },
      actorRef => Serialization.serializedActorPath(actorRef.toUntyped)
    )

  implicit lazy val locationCodec: Codec[Location]           = deriveCodec[Location]
  implicit lazy val trackingEventCodec: Codec[TrackingEvent] = deriveCodec[TrackingEvent]
}
