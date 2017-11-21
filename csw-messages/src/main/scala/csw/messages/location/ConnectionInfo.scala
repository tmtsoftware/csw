package csw.messages.location

import play.api.libs.json.{Json, OFormat}

private[location] case class ConnectionInfo(name: String, componentType: ComponentType, connectionType: ConnectionType) {
  override def toString: String = s"$name-${componentType.name}-${connectionType.name}"
}

private[location] object ConnectionInfo {
  implicit val connectionInfoFormat: OFormat[ConnectionInfo] = Json.format[ConnectionInfo]
}
