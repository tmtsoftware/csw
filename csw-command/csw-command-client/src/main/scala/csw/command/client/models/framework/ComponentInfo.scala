package csw.command.client.models.framework

import csw.location.api.models.{ComponentType, Connection}
import csw.prefix.models.Prefix

import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.jdk.CollectionConverters._

/**
 * The information needed to create a component. This class is created after de-serializing the config file for the component.
 *
 * @param prefix prefix for the component with `subsystem` and `name`
 * @param componentType
 *  : the type of the component as defined by [[csw.location.api.models.ComponentType]]
 * @param behaviorFactoryClassName
 *  : specifies the component to be created by name of the class of it's factory
 * @param locationServiceUsage
 *  : specifies component's usage of location service
 * @param connections : set of connections that will be used by this component for interaction
 * @param initializeTimeout
 *  : the timeout value used while initializing a component
 */
final case class ComponentInfo(
    prefix: Prefix,
    componentType: ComponentType,
    behaviorFactoryClassName: String,
    locationServiceUsage: LocationServiceUsage,
    connections: Set[Connection] = Set.empty,
    initializeTimeout: FiniteDuration = 10.seconds
) {

  /**
   * Java API to get the list of connections for the assembly
   */
  def getConnections: java.util.List[Connection] = connections.toList.asJava
}
