package csw.command.client.models.framework

import csw.location.models.{ComponentType, Connection}
import csw.params.core.models.{Prefix, Subsystem}

import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.jdk.CollectionConverters._

/**
 * The information needed to create a component. This class is created after de-serializing the config file for the component.
 *
 * @param name the name of the component
 * @param subsystem identifies the subsystem
 * @param componentType
 *  : the type of the component as defined by [[csw.location.models.ComponentType]]
 * @param behaviorFactoryClassName
 *  : specifies the component to be created by name of the class of it's factory
 * @param locationServiceUsage
 *  : specifies component's usage of location service
 * @param connections : set of connections that will be used by this component for interaction
 * @param initializeTimeout
 *  : the timeout value used while initializing a component
 */
final case class ComponentInfo(
    name: String,
    subsystem: Subsystem,
    componentType: ComponentType,
    behaviorFactoryClassName: String,
    locationServiceUsage: LocationServiceUsage,
    connections: Set[Connection] = Set.empty,
    initializeTimeout: FiniteDuration = 10.seconds
) {
  val prefix = Prefix(subsystem, name)

  /**
   * Java API to get the list of connections for the assembly
   */
  def getConnections: java.util.List[Connection] = connections.toList.asJava
}
