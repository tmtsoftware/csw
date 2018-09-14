package csw.services.location.api.formats

import java.net.URI

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.serialization.{Serialization, SerializationExtension}
import csw.params.extensions.Formats
import csw.params.extensions.Formats.MappableFormat
import csw.services.location.api.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.api.models.{Location, TrackingEvent}
import julienrf.json.derived
import play.api.libs.json.{__, Format, Json, OFormat}

private[csw] trait ActorSystemDependentFormats {
  implicit def actorSystem: ActorSystem

  implicit val akkaConnectionFormat: Format[AkkaConnection] = Json.format[AkkaConnection]
  implicit val tcpConnectionFormat: Format[TcpConnection]   = Json.format[TcpConnection]
  implicit val httpConnectionFormat: Format[HttpConnection] = Json.format[HttpConnection]

  implicit val uriFormat: Format[URI] = Formats.of[String].bimap[URI](_.toString, new URI(_))

  implicit def actorRefFormat[T]: Format[ActorRef[T]] =
    Formats
      .of[String]
      .bimap[ActorRef[T]](
        actorRef => Serialization.serializedActorPath(actorRef.toUntyped),
        path => {
          val provider = SerializationExtension(actorSystem).system.provider
          provider.resolveActorRef(path)
        }
      )

  implicit val locationFormat: OFormat[Location]           = derived.flat.oformat((__ \ "type").format[String])
  implicit val trackingEventFormat: OFormat[TrackingEvent] = derived.flat.oformat((__ \ "type").format[String])
}
