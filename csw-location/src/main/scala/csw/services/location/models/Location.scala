package csw.services.location.models

import java.net.URI

import akka.actor.{ActorPath, ActorRef}
import akka.serialization.Serialization
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}

/**
  * Represents a `Location` with [[csw.services.location.models.Connection]] and `URI`
  */
sealed abstract class Location extends TmtSerializable {
  /**
    * A `Connection` for which the `Location` will be formed
    */
  def connection: Connection

  /**
    * An `URI` of the offered service e.g. http, tcp or akka
    */
  def uri: URI
}

/**
  * Represents an `Akka` type of location
  *
  * @param connection An `AkkaConnection` for which `Location` will be formed
  * @param uri        An `URI` of the offered `AkkaConnection`
  * @param actorRef   An `ActorRef` offered for communication
  */
final case class AkkaLocation(connection: AkkaConnection, uri: URI, actorRef: ActorRef) extends Location

/**
  * Represents a `Tcp` type of location
  *
  * @param connection A `TcpConnection` for which `Location` will be formed
  * @param uri        An `URI` of the offered `TcpConnection`
  */
final case class TcpLocation(connection: TcpConnection, uri: URI) extends Location

/**
  * Represents a `Http` type of location
  *
  * @param connection A `TcpConnection` for which `Location` will be formed
  * @param uri        An `URI` of the offered `HttpConnection`
  */
final case class HttpLocation(connection: HttpConnection, uri: URI) extends Location
