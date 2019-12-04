package csw.framework.javadsl

import java.time.Duration
import java.util

import csw.command.client.models.framework.{ComponentInfo, LocationServiceUsage}
import csw.location.models.{ComponentType, Connection}
import csw.prefix.Subsystem

import scala.compat.java8.DurationConverters.DurationOps
import scala.jdk.CollectionConverters._

/**
 * Helper instance for Java to create [[csw.command.client.models.framework.ComponentInfo]]
 */
object JComponentInfo {

  /**
   * The information needed to create a component. This class is created after de-serializing the config file for the component.
   *
   * @param name the name of the component
   * @param subsystem identifies the subsystem
   * @param componentType the type of the component as defined by [[csw.location.models.ComponentType]]
   * @param className specifies the component to be created by name of the class of it's factory
   * @param locationServiceUsage specifies component's usage of location service
   * @param connections set of connections that will be used by this component for interaction
   * @param initializeTimeout the timeout value used while initializing a component
   * @return an instance of ComponentInfo
   */
  def from(
      name: String,
      subsystem: Subsystem,
      componentType: ComponentType,
      className: String,
      locationServiceUsage: LocationServiceUsage,
      connections: util.Set[Connection],
      initializeTimeout: Duration
  ): ComponentInfo = ComponentInfo(
    name,
    subsystem,
    componentType,
    className,
    locationServiceUsage,
    connections.asScala.toSet,
    initializeTimeout.toScala
  )
}
