package csw.services.location.exceptions

import csw.services.location.models.{Connection, Location}

case class RegistrationFailed(connection: Connection) extends RuntimeException(
  s"unable to register $connection"
)

case class UnregistrationFailed(connection: Connection) extends RuntimeException(
  s"unable to unregister $connection"
)

case class OtherLocationIsRegistered(location: Location, otherLocation: Location) extends RuntimeException(
  s"there is other location=$otherLocation registered against name=${location.connection.name}."
)

case object RegistrationListingFailed extends RuntimeException(
  s"unable to get the list of registered locations"
)
