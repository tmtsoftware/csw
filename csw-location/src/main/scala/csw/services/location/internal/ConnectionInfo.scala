package csw.services.location.internal

import play.api.libs.json.{Json, OFormat}

private[location] case class ConnectionInfo(name: String, componentType: String, connectionType: String) {
  override def toString: String = s"$name-$componentType-$connectionType"
}

private[location] object ConnectionInfo {
  implicit val componentInfoFormat: OFormat[ConnectionInfo] = Json.format[ConnectionInfo]
}
