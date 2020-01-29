package csw.location.api.models

import java.net.URI

import csw.location.api.codec.LocationSerializable
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.prefix.models.Prefix

/**
 * Location represents a live Connection along with its URI
 */
sealed abstract class Location extends LocationSerializable {

  /**
   * Represents a connection based on a componentId and the type of connection offered by the component
   */
  def connection: Connection

  /**
   * Represents the URI of the component
   */
  def uri: URI

  /**
   * Represents the fully qualified component name along with the subsystem for e.g. tcs.filter.wheel
   */
  def prefix: Prefix = connection.connectionInfo.prefix

}

/**
 * Represents a live Akka connection of an Actor
 *
 * @note Do not directly access actorRef from constructor, use one of component() or containerRef() method
 *       to get the correctly typed actor reference.
 * @param connection represents a connection based on a componentId and the type of connection offered by the component
 * @param uri represents the actor URI of the component. Gateway or router for a component that other components will resolve and talk to.
 */
final case class AkkaLocation(connection: AkkaConnection, uri: URI) extends Location

/**
 * Represents a live Tcp connection
 *
 * @param connection represents a connection based on a componentId and the type of connection offered by the component
 * @param uri represents the remote URI of the component that other components will resolve and talk to
 */
final case class TcpLocation(connection: TcpConnection, uri: URI) extends Location

/**
 * Represents a live Http connection
 *
 * @param connection represents a connection based on a componentId and the type of connection offered by the component
 * @param uri represents the remote URI of the component that other components will resolve and talk to
 */
final case class HttpLocation(connection: HttpConnection, uri: URI) extends Location
