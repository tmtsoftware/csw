package csw.messages.location

import java.net.URI

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.serialization.{Serialization, SerializationExtension}
import csw.messages.extensions.Formats
import csw.messages.extensions.Formats.MappableFormat
import csw.messages.location.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import julienrf.json.derived
import play.api.libs.json._

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
