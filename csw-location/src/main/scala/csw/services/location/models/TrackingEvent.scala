package csw.services.location.models

sealed abstract class TrackingEvent {
  def connection: Connection
}

case class LocationUpdated(location: Location) extends TrackingEvent {
  override def connection: Connection = location.connection
}
case class LocationRemoved(connection: Connection) extends TrackingEvent
