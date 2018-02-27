package csw.messages.location

/**
 * TrackingEvent is used to represent location events while tracking the connection
 */
sealed abstract class TrackingEvent {
  def connection: Connection
}

/**
 * This event represents modification in location details
 */
//TODO:explain why we have location in LocationUpdated
case class LocationUpdated(location: Location) extends TrackingEvent {
  override def connection: Connection = location.connection
}

/**
 * This event represents unavailability of a location
 */
//TODO:explain why we have only Connection in LocationRemoved
case class LocationRemoved(connection: Connection) extends TrackingEvent
