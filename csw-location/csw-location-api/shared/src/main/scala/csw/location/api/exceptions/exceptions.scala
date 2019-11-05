package csw.location.api.exceptions

import java.net.URI

import csw.location.models.{Connection, Location}

sealed trait RegistrationError extends Throwable

/**
 * An Exception representing failure in registration
 *
 */
case class RegistrationFailed(msg: String) extends RuntimeException(msg) with RegistrationError {
  def this(connection: Connection) = this(s"unable to register $connection")
}

/**
 * An Exception representing failure in registration as other location is already registered in place of the given location
 *
 */
case class OtherLocationIsRegistered(msg: String) extends RuntimeException(msg) with RegistrationError {
  def this(location: Location, otherLocation: Location) =
    this(s"there is other location=$otherLocation registered against name=${location.connection.name}.")
}

/**
 * An Exception representing failure in un-registration
 *
 * @param connection a connection for which un-registration failed
 */
case class UnregistrationFailed(connection: Connection) extends RuntimeException(s"unable to unregister $connection")

/**
 * An Exception representing failure in registering non remote actors
 *
 * @param actorRefURI the reference of the Actor that is expected to be remote but instead it is local
 */
case class LocalAkkaActorRegistrationNotAllowed(actorRefURI: URI)
    extends RuntimeException(s"Registration of only remote actors is allowed. Instead local actor $actorRefURI received.")

/**
 * An Exception representing failure in listing locations
 */
case class RegistrationListingFailed() extends RuntimeException(s"unable to get the list of registered locations")

/**
 * Represents the current node is not able to join the cluster
 */
case class CouldNotJoinCluster() extends RuntimeException("could not join cluster")
