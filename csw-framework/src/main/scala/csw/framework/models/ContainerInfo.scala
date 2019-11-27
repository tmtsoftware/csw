package csw.framework.models

import csw.command.client.models.framework.ComponentInfo
import csw.params.core.models.{Prefix, Subsystem}

/**
 * Container information as represented in the configuration file
 *
 * @param name name of the container
 * @param components set of components to be created inside this container
 */
private[framework] final case class ContainerInfo(name: String, components: Set[ComponentInfo]) {
  require(components.nonEmpty, "components can not be empty.")

  /**
   * Represents the prefix for the container with [[Subsystem.Container]] and given `name`
   */
  val prefix = Prefix(Subsystem.Container, name)
}
