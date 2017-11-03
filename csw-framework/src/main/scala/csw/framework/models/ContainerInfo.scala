package csw.framework.models

import csw.messages.framework.ComponentInfo
import play.api.libs.json._

/**
 * Container information as represented in the configuration file
 * @param name         Name of the container
 * @param components   Set of components to be created inside this container
 */
final case class ContainerInfo(name: String, components: Set[ComponentInfo]) {
  require(components.nonEmpty, "components can not be empty.")
}

case object ContainerInfo {
  implicit val format: OFormat[ContainerInfo] = Json.format[ContainerInfo]
}
