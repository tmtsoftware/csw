package csw.services.location.models

import java.net.URI

import acyclic.skipped
import akka.typed
import akka.typed.ActorRef
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}

/**
 * Location represents a live Connection along with its URI
 */
sealed abstract class Location extends TmtSerializable {
  def connection: Connection
  def uri: URI
}

/**
 * Represents a live Akka connection of an Actor
 */
final case class AkkaLocation(connection: AkkaConnection, uri: URI, actorRef: ActorRef[_]) extends Location {
  def typedRef[T]: typed.ActorRef[T] = actorRef.asInstanceOf[ActorRef[T]]
}

/**
 * Represents a live Tcp connection
 */
final case class TcpLocation(connection: TcpConnection, uri: URI) extends Location

/**
 * Represents a live Http connection
 */
final case class HttpLocation(connection: HttpConnection, uri: URI) extends Location
