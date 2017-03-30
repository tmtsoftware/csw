package csw.services.location.exceptions

import csw.services.location.models.{Connection, Location}

/**
  * An Exception representing failure in registration
  *
  * @param connection A `Connection` for which registration failed
  */
case class RegistrationFailed(connection: Connection) extends RuntimeException(
  s"unable to register $connection"
)

/**
  * An Exception representing failure in un-registration
  *
  * @param connection A `Connection` for which un-registration failed
  */
case class UnregistrationFailed(connection: Connection) extends RuntimeException(
  s"unable to unregister $connection"
)

/**
  * An Exception representing failure in registration because of some other location registered for the same `Connection`
  *
  * @param location      The `Location` that component tried to register and failed
  * @param otherLocation The `Location` which was already present against the `Connection` that component was trying to register
  */
case class OtherLocationIsRegistered(location: Location, otherLocation: Location) extends RuntimeException(
  s"there is other location=$otherLocation registered against name=${location.connection.name}."
)

/**
  * An Exception representing failure in listing locations
  */
case object RegistrationListingFailed extends RuntimeException(
  s"unable to get the list of registered locations"
)
