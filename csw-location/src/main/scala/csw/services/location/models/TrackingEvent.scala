package csw.services.location.models

/**
  * TrackingEvent is used to represent events for tracking a connection
  */
sealed abstract class TrackingEvent {
  def connection: Connection
}

/**
  * This event represents modification in location details
  */
case class LocationUpdated(location: Location) extends TrackingEvent {
  override def connection: Connection = location.connection
}

/**
  * This event represents unavailability of a location
  */
case class LocationRemoved(connection: Connection) extends TrackingEvent
