package csw.framework.models

import csw.messages.framework.ComponentInfo
import play.api.libs.json._

final case class ContainerInfo(name: String, components: Set[ComponentInfo]) {
  require(components.nonEmpty, "components can not be empty.")
}

case object ContainerInfo {
  implicit val format: OFormat[ContainerInfo] = Json.format[ContainerInfo]
}
