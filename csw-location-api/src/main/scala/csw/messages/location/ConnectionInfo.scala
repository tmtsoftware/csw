package csw.messages.location

import play.api.libs.json.{Json, OFormat}

/**
 * ConnectionInfo represents a component name, component type and connection type
 *
 * @param name represents a component name e.g. TromboneAssembly
 * @param componentType represents the type of component e.g. Assembly, HCD, etc
 * @param connectionType represents the type of connection e.g. akka, http, tcp
 */
case class ConnectionInfo private[location] (name: String, componentType: ComponentType, connectionType: ConnectionType) {
  override def toString: String = s"$name-${componentType.name}-${connectionType.name}"
}

private[location] object ConnectionInfo {
  private[messages] implicit val connectionInfoFormat: OFormat[ConnectionInfo] = Json.format[ConnectionInfo]
}
