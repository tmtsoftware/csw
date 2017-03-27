package csw.services.location.models

import java.net.URI

import akka.actor.{ActorPath, ActorRef}
import akka.serialization.Serialization
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}

sealed abstract class Location extends TmtSerializable {
  def connection: Connection
  def uri: URI
}

final case class AkkaLocation(connection: AkkaConnection, uri: URI, actorRef: ActorRef) extends Location

final case class TcpLocation(connection: TcpConnection, uri: URI) extends Location

final case class HttpLocation(connection: HttpConnection, uri: URI) extends Location
