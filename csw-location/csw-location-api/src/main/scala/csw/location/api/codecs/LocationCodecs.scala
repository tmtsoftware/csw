package csw.location.api.codecs

import java.net.URI

import akka.Done
import akka.actor.typed.scaladsl.adapter.{TypedActorRefOps, TypedActorSystemOps, _}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.serialization.{Serialization, SerializationExtension}
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.location.api.models._
import csw.params.core.formats.{CommonCodecs, CborHelpers}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs._

trait LocationCodecs extends CommonCodecs {
  implicit def actorSystem: ActorSystem[_]

  implicit lazy val connectionTypeCodec: Codec[ConnectionType] = CborHelpers.enumCodec[ConnectionType]
  implicit lazy val componentTypeCodec: Codec[ComponentType]   = CborHelpers.enumCodec[ComponentType]
  implicit lazy val componentIdCodec: Codec[ComponentId]       = deriveCodec[ComponentId]
  implicit lazy val connectionInfoCodec: Codec[ConnectionInfo] = deriveCodec[ConnectionInfo]

  implicit lazy val connectionCodec: Codec[Connection] =
    CborHelpers.bimap[ConnectionInfo, Connection](Connection.from, _.connectionInfo)

  implicit lazy val akkaConnectionCodec: Codec[AkkaConnection] = deriveCodec[AkkaConnection]
  implicit lazy val httpConnectionCodec: Codec[HttpConnection] = deriveCodec[HttpConnection]
  implicit lazy val tcpConnectionCodec: Codec[TcpConnection]   = deriveCodec[TcpConnection]
//  implicit lazy val typedConnectionCodec: Codec[TypedConnection]   = deriveCodec[TypedConnection]

  implicit lazy val uriCodec: Codec[URI] = CborHelpers.bimap[String, URI](new URI(_), _.toString)

  implicit def actorRefCodec[T]: Codec[ActorRef[T]] =
    CborHelpers.bimap[String, ActorRef[T]](
      path => {
        val provider = SerializationExtension(actorSystem.toUntyped).system.provider
        provider.resolveActorRef(path)
      },
      actorRef => Serialization.serializedActorPath(actorRef.toUntyped)
    )

  implicit lazy val locationCodec: Codec[Location]         = deriveCodec[Location]
  implicit lazy val akkaLocationCodec: Codec[AkkaLocation] = deriveCodec[AkkaLocation]
  implicit lazy val httpLocationCodec: Codec[HttpLocation] = deriveCodec[HttpLocation]

  implicit lazy val registrationCodec: Codec[Registration]         = deriveCodec[Registration]
  implicit lazy val akkaRegistrationCodec: Codec[AkkaRegistration] = deriveCodec[AkkaRegistration]
  implicit lazy val tcpRegistrationCodec: Codec[TcpRegistration]   = deriveCodec[TcpRegistration]
  implicit lazy val httpRegistrationCodec: Codec[HttpRegistration] = deriveCodec[HttpRegistration]

  implicit lazy val trackingEventCodec: Codec[TrackingEvent]     = deriveCodec[TrackingEvent]
  implicit lazy val locationUpdatedCodec: Codec[LocationUpdated] = deriveCodec[LocationUpdated]
  implicit lazy val locationRemovedCodec: Codec[LocationRemoved] = deriveCodec[LocationRemoved]

  implicit lazy val doneCodec: Codec[Done] = CborHelpers.bimap[String, Done](_ => Done, _ => "done")
}
