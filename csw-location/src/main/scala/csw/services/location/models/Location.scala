package csw.services.location.models

import java.net.URI

import akka.actor.{ActorPath, ActorRef}
import akka.serialization.Serialization
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}

sealed abstract class Location extends TmtSerializable {
  def connection: Connection

  def uri: URI
}

final case class Removed(connection: Connection) extends Location {
  override def uri: URI = ???
}

final case class ResolvedAkkaLocation private (connection: AkkaConnection, uri: URI, actorRef: ActorRef) extends Location {
  def this(connection: AkkaConnection, actorRef: ActorRef) = {
    this(connection, new URI(ActorPath.fromString(Serialization.serializedActorPath(actorRef)).toString), actorRef)
  }
}

final case class ResolvedTcpLocation private (connection: TcpConnection, uri: URI) extends Location {
  def this(connection: TcpConnection, hostname: String, port: Int) = {
    this(connection, new URI(s"tcp://$hostname:$port"))
  }
}

final case class ResolvedHttpLocation private (connection: HttpConnection, uri: URI) extends Location {
  def this(connection: HttpConnection, hostname: String, port: Int, path: String) = {
    this(connection, new URI(s"http://$hostname:$port/$path"))
  }
}
