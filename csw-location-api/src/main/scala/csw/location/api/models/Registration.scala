package csw.location.api.models

import java.net.URI

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter.TypedActorRefOps
import akka.actor.{ActorPath, Address}
import akka.serialization.Serialization
import csw.location.api.commons.LocationServiceLogger
import csw.params.core.models.Prefix
import csw.location.api.exceptions.LocalAkkaActorRegistrationNotAllowed
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.logging.messages.LogControlMessages
import csw.logging.scaladsl.Logger

/**
 * Registration holds information about a connection and its live location. This model is used to register a connection with LocationService.
 */
sealed abstract class Registration {

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
 * @param actorRef Provide a remote actor that is offering a connection. Local actors cannot be registered since they can't be
 *                 communicated from components across the network
 * @param logAdminActorRef the ActorRef responsible to handle log level change for a component dynamically
 */
final case class AkkaRegistration(
    connection: AkkaConnection,
    prefix: Prefix,
    actorRef: ActorRef[Nothing],
    logAdminActorRef: ActorRef[LogControlMessages]
) extends Registration {

  private val log: Logger = LocationServiceLogger.getLogger

  // ActorPath represents the akka path of an Actor
  private val actorPath = ActorPath.fromString(Serialization.serializedActorPath(actorRef.toUntyped))

  // Prepare the URI from the ActorPath. Allow only the remote actor to be registered with LocationService
  private val uri = {
    actorPath.address match {
      case Address(_, _, None, None) =>
        val registrationNotAllowed = LocalAkkaActorRegistrationNotAllowed(actorRef)
        log.error(registrationNotAllowed.getMessage, ex = registrationNotAllowed)
        throw registrationNotAllowed
      case _ => new URI(actorPath.toString)
    }
  }

  /**
   * Create a AkkaLocation that represents the live connection offered by the actor
   *
   * @param hostname provide a hostname where the connection endpoint is available
   * @return an AkkaLocation location representing a live connection at provided hostname
   */
  override def location(hostname: String): Location = AkkaLocation(connection, prefix, uri, actorRef, logAdminActorRef)
}

/**
 * TcpRegistration holds information needed to register a Tcp service
 *
 * @param port provide the port where Tcp service is available
 */
final case class TcpRegistration(connection: TcpConnection, port: Int, logAdminActorRef: ActorRef[LogControlMessages])
    extends Registration {

  /**
   * Create a TcpLocation that represents the live Tcp service
   *
   * @param hostname provide the hostname where Tcp service is available
   * @return an TcpLocation location representing a live connection at provided hostname
   */
  override def location(hostname: String): Location =
    TcpLocation(connection, new URI(s"tcp://$hostname:$port"), logAdminActorRef)
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
    path: String,
    logAdminActorRef: ActorRef[LogControlMessages]
) extends Registration {

  /**
   * Create a HttpLocation that represents the live Http service
   *
   * @param hostname provide the hostname where Http service is available
   */
  override def location(hostname: String): Location =
    HttpLocation(connection, new URI(s"http://$hostname:$port/$path"), logAdminActorRef)
}
