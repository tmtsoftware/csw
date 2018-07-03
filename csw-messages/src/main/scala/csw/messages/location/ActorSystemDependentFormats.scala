package csw.messages.location

import java.net.URI

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.serialization.{Serialization, SerializationExtension}
import csw.messages.location.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import upickle.default.{macroRW, ReadWriter => RW}

private[csw] trait ActorSystemDependentFormats {
  implicit def actorSystem: ActorSystem

  implicit val akkaConnectionRw: RW[AkkaConnection] = macroRW
  implicit val tcpConnectionRw: RW[TcpConnection]   = macroRW
  implicit val httpConnectionRw: RW[HttpConnection] = macroRW
  implicit val connectionRw: RW[Connection]         = RW.merge(akkaConnectionRw, tcpConnectionRw, httpConnectionRw)

  implicit val uriRw: RW[URI] = upickle.default.readwriter[String].bimap[URI](_.toString, new URI(_))

  implicit def actorRefRw[T]: RW[ActorRef[T]] =
    upickle.default
      .readwriter[String]
      .bimap[ActorRef[T]](
        actorRef => Serialization.serializedActorPath(actorRef.toUntyped),
        path => {
          val provider = SerializationExtension(actorSystem).system.provider
          provider.resolveActorRef(path)
        }
      )

  implicit val akkaLocationRw: RW[AkkaLocation] = macroRW
  implicit val tcpLocationRw: RW[TcpLocation]   = macroRW
  implicit val httpLocationRw: RW[HttpLocation] = macroRW
  implicit val locationRw: RW[Location]         = RW.merge(httpLocationRw, akkaLocationRw, tcpLocationRw)

  implicit val locationRemovedRw: RW[LocationRemoved] = macroRW
  implicit val locationUpdatedRw: RW[LocationUpdated] = macroRW
  implicit val trackingEventRw: RW[TrackingEvent]     = RW.merge(locationRemovedRw, locationUpdatedRw)
}
