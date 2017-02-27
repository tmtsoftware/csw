package csw.services.location.scaladsl

import javax.jmdns.ServiceEvent

trait ConnectionState {
  def event: ServiceEvent
}

object ConnectionState {
  case class ConnectionAdded(event: ServiceEvent) extends ConnectionState
  case class ConnectionRemoved(event: ServiceEvent) extends ConnectionState
  case class ConnectionResolved(event: ServiceEvent) extends ConnectionState
}
