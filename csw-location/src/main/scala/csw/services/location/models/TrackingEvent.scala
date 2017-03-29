package csw.services.location.models

/**
  * Represents `Tracking events` for a given `Connection`
  */
sealed abstract class TrackingEvent {
  def connection: Connection
}

/**
  * Represents `Update` event for a `Connection`
  *
  * @param location A `Location` that got updated
  */
case class LocationUpdated(location: Location) extends TrackingEvent {
  override def connection: Connection = location.connection
}

/**
  * Represents `Removed` event for a `Connection`
  *
  * @param connection A `Connection` for which `Location` was removed
  */
case class LocationRemoved(connection: Connection) extends TrackingEvent
