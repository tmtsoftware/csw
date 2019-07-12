package csw.location.api.models

import java.net.URI

import csw.location.api.codecs.LocationSerializable
import csw.location.api.commons.LocationServiceLogger
import csw.location.api.exceptions.LocalAkkaActorRegistrationNotAllowed
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.logging.api.scaladsl.Logger
import csw.params.core.models.Prefix

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
}

/**
 * AkkaRegistration holds the information needed to register an akka location. A [[csw.location.api.exceptions.LocalAkkaActorRegistrationNotAllowed]]
 * is thrown if the actorRef provided is not a remote actorRef
 *
 * @param connection the `Connection` to register with `LocationService`
 * @param prefix prefix of the component
 * @param actorRefURI Provide a remote actor that is offering a connection. Local actors cannot be registered since they can't be
 *                 communicated from components across the network
 */
final case class AkkaRegistration(
    connection: AkkaConnection,
    prefix: Prefix,
    actorRefURI: URI
) extends Registration {

  if (actorRefURI.getPort == -1) {
    val log: Logger            = LocationServiceLogger.getLogger
    val registrationNotAllowed = LocalAkkaActorRegistrationNotAllowed(actorRefURI)
    log.error(registrationNotAllowed.getMessage, ex = registrationNotAllowed)
    throw registrationNotAllowed
  }

  /**
   * Create a AkkaLocation that represents the live connection offered by the actor
   *
   * @param hostname provide a hostname where the connection endpoint is available
   * @return an AkkaLocation location representing a live connection at provided hostname
   */
  override def location(hostname: String): Location = AkkaLocation(connection, prefix, actorRefURI)
}

/**
 * TcpRegistration holds information needed to register a Tcp service
 *
 * @param port provide the port where Tcp service is available
 */
final case class TcpRegistration(connection: TcpConnection, port: Int) extends Registration {

  /**
   * Create a TcpLocation that represents the live Tcp service
   *
   * @param hostname provide the hostname where Tcp service is available
   * @return an TcpLocation location representing a live connection at provided hostname
   */
  override def location(hostname: String): Location =
    TcpLocation(connection, new URI(s"tcp://$hostname:$port"))
}

/**
 * HttpRegistration holds information needed to register a Http service
 *
 * @param port provide the port where Http service is available
 * @param path provide the path to reach the available http service
 */
final case class HttpRegistration(
    connection: HttpConnection,
    port: Int,
    path: String
) extends Registration {

  /**
   * Create a HttpLocation that represents the live Http service
   *
   * @param hostname provide the hostname where Http service is available
   */
  override def location(hostname: String): Location =
    HttpLocation(connection, new URI(s"http://$hostname:$port/$path"))
}
