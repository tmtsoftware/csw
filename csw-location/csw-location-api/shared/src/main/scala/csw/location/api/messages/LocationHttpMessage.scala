package csw.location.api.messages

import csw.location.models._

import scala.concurrent.duration.FiniteDuration

sealed trait LocationHttpMessage

object LocationHttpMessage {
  case class Register(registration: Registration)                                   extends LocationHttpMessage
  case class Unregister(connection: Connection)                                     extends LocationHttpMessage
  case object UnregisterAll                                                         extends LocationHttpMessage
  case class Find(connection: TypedConnection[Location])                            extends LocationHttpMessage
  case class Resolve(connection: TypedConnection[Location], within: FiniteDuration) extends LocationHttpMessage
  case object ListEntries                                                           extends LocationHttpMessage
  case class ListByComponentType(componentType: ComponentType)                      extends LocationHttpMessage
  case class ListByHostname(hostname: String)                                       extends LocationHttpMessage
  case class ListByConnectionType(connectionType: ConnectionType)                   extends LocationHttpMessage
  case class ListByPrefix(prefix: String)                                           extends LocationHttpMessage
}
