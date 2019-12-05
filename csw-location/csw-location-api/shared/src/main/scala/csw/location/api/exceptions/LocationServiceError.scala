package csw.location.api.exceptions

import csw.location.models.{Connection, Location}

sealed abstract class LocationServiceError(msg: String) extends RuntimeException(msg)

/**
 * An Exception representing failure in registration
 *
 */
case class RegistrationFailed(msg: String) extends LocationServiceError(msg) {
  def this(connection: Connection) = this(s"unable to register $connection")
}

/**
 * An Exception representing failure in registration as other location is already registered in place of the given location
 *
 */
case class OtherLocationIsRegistered(msg: String) extends LocationServiceError(msg) {
  def this(location: Location, otherLocation: Location) =
    this(s"there is other location=$otherLocation registered against name=${location.connection.name}.")
}

/**
 * An Exception representing failure in un-registration
 *
 * @param connection a connection for which un-registration failed
 */
case class UnregistrationFailed(connection: Connection) extends LocationServiceError(s"unable to unregister $connection")

/**
 * An Exception representing failure in listing locations
 */
case class RegistrationListingFailed() extends LocationServiceError(s"unable to get the list of registered locations")
