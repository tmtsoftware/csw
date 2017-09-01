package csw.services.location.internal

import csw.services.location.models.{ComponentType, ConnectionType}
import play.api.libs.json.{Json, OFormat}

private[location] case class ConnectionInfo(name: String, componentType: ComponentType, connectionType: ConnectionType) {
  override def toString: String = s"$name-${componentType.name}-${connectionType.name}"
}

private[location] object ConnectionInfo {
  implicit val connectionInfoFormat: OFormat[ConnectionInfo] = Json.format[ConnectionInfo]
}
