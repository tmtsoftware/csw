package csw.services.location.models

import java.net.URI

import akka.actor.ActorRef
import akka.typed
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import acyclic.skipped

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
final case class AkkaLocation(connection: AkkaConnection, uri: URI, actorRef: ActorRef) extends Location {
  import akka.typed.scaladsl.adapter._
  def typedRef[T]: typed.ActorRef[T] = actorRef
}

/**
 * Represents a live Tcp connection
 */
final case class TcpLocation(connection: TcpConnection, uri: URI) extends Location

/**
 * Represents a live Http connection
 */
final case class HttpLocation(connection: HttpConnection, uri: URI) extends Location
