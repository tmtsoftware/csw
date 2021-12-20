package csw.location.api.models

import java.net.URI

import csw.location.api.codec.LocationSerializable
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}

/**
 * Registration holds information about a connection and its live location. This model is used to register a connection with LocationService.
 */
sealed abstract class Registration extends LocationSerializable {

  /**
   * The `Connection` to register with `LocationService`
   */
  def connection: Connection

  /**
   * A location represents a live connection available for consumption
   *
   * @param hostname provide a hostname where the connection endpoint is available
   * @return a location representing a live connection at provided hostname
   */
  def location(hostname: String): Location

  /**
   * metadata represents any additional information (metadata) associated with registration
   */
  def metadata: Metadata

  def withCswVersion(version: String): Registration

}

/**
 * AkkaRegistration holds the information needed to register an akka location
 *
 * @param connection the `Connection` to register with `LocationService`
 * @param actorRefURI Provide a remote actor uri that is offering a connection. Local actors cannot be registered since they can't be
 *                 communicated from components across the network
 * @param metadata represents additional metadata information associated with location. Defaulted to empty if not provided.
 */
final case class AkkaRegistration private[csw] (
    connection: AkkaConnection,
    actorRefURI: URI,
    metadata: Metadata
) extends Registration {

  /**
   * Create a AkkaLocation that represents the live connection offered by the actor
   *
   * @param hostname provide a hostname where the connection endpoint is available
   * @return an AkkaLocation location representing a live connection at provided hostname
   */
  override def location(hostname: String): Location = AkkaLocation(connection, actorRefURI, metadata)

  override def withCswVersion(version: String): AkkaRegistration = this.copy(metadata = metadata.withCSWVersion(version))

}

/**
 * TcpRegistration holds information needed to register a Tcp service
 *
 * @param port provide the port where Tcp service is available
 * @param metadata represents additional metadata information associated with location. Defaulted to empty if not provided.
 */
final case class TcpRegistration(connection: TcpConnection, port: Int, metadata: Metadata) extends Registration {

  //Used for JAVA API
  def this(connection: TcpConnection, port: Int) = this(connection, port, Metadata.empty)

  /**
   * Create a TcpLocation that represents the live Tcp service
   *
   * @param hostname provide the hostname where Tcp service is available
   * @return an TcpLocation location representing a live connection at provided hostname
   */
  override def location(hostname: String): Location = TcpLocation(connection, new URI(s"tcp://$hostname:$port"), metadata)

  override def withCswVersion(version: String): TcpRegistration = this.copy(metadata = metadata.withCSWVersion(version))
}

object TcpRegistration {
  def apply(connection: TcpConnection, port: Int): TcpRegistration = new TcpRegistration(connection, port, Metadata.empty)
}

/**
 * HttpRegistration holds information needed to register a Http service
 *
 * @param port provide the port where Http service is available
 * @param path provide the path to reach the available http service
 * @param metadata represents additional metadata information associated with location. Defaulted to empty if not provided.
 */
final case class HttpRegistration(
    connection: HttpConnection,
    port: Int,
    path: String,
    networkType: NetworkType,
    metadata: Metadata
) extends Registration {

  //Used for JAVA API
  def this(connection: HttpConnection, port: Int, path: String, metadata: Metadata) =
    this(connection, port, path, NetworkType.Inside, metadata)

  def this(connection: HttpConnection, port: Int, path: String) =
    this(connection, port, path, NetworkType.Inside, Metadata.empty)

  def this(connection: HttpConnection, port: Int, path: String, networkType: NetworkType) =
    this(connection, port, path, networkType, Metadata.empty)

  /**
   * Create a HttpLocation that represents the live Http service
   *
   * @param hostname provide the hostname where Http service is available
   */
  override def location(hostname: String): Location = {
    HttpLocation(connection, new URI(s"http://$hostname:$port/$path"), metadata)
  }

  override def withCswVersion(version: String): HttpRegistration = this.copy(metadata = metadata.withCSWVersion(version))
}

object HttpRegistration {
  def apply(connection: HttpConnection, port: Int, path: String, metadata: Metadata): HttpRegistration =
    new HttpRegistration(connection, port, path, NetworkType.Inside, metadata)

  def apply(connection: HttpConnection, port: Int, path: String, networkType: NetworkType): HttpRegistration =
    new HttpRegistration(connection, port, path, networkType, Metadata.empty)

  def apply(
      connection: HttpConnection,
      port: Int,
      path: String
  ): HttpRegistration =
    new HttpRegistration(connection, port, path, NetworkType.Inside, Metadata.empty)
}
