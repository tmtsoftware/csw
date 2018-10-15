package csw.framework.models

import csw.command.client.internal.models.framework.ComponentInfo
import play.api.libs.json._

/**
 * Container information as represented in the configuration file
 *
 * @param name name of the container
 * @param components set of components to be created inside this container
 */
private[framework] final case class ContainerInfo(name: String, components: Set[ComponentInfo]) {
  require(components.nonEmpty, "components can not be empty.")
}

case object ContainerInfo {
  private[csw] implicit val format: OFormat[ContainerInfo] = Json.format[ContainerInfo]
}
