package csw.services.location.internal

import spray.json.JsonFormat

private[location] case class ConnectionInfo(name: String, componentType: String, connectionType: String) {
  override def toString: String = s"$name-$componentType-$connectionType"
}

private[location] object ConnectionInfo {
  import JsonSupport._
  implicit val format: JsonFormat[ConnectionInfo] = jsonFormat3(ConnectionInfo.apply)
}
