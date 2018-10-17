package csw.framework.javadsl

import java.time.Duration
import java.util

import csw.command.client.models.framework.{ComponentInfo, LocationServiceUsage}
import csw.location.api.models.{ComponentType, Connection}
import csw.params.core.models.Prefix

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.compat.java8.DurationConverters.DurationOps

/**
 * Helper instance for Java to create [[csw.command.client.models.framework.ComponentInfo]]
 */
object JComponentInfo {

  /**
   * The information needed to create a component. This class is created after de-serializing the config file for the component.
   *
   * @param name the name of the component
   * @param componentType the type of the component as defined by [[csw.location.api.models.ComponentType]]
   * @param prefix identifies the subsystem
   * @param className specifies the component to be created by name of the class of it's factory
   * @param locationServiceUsage specifies component's usage of location service
   * @param connections set of connections that will be used by this component for interaction
   * @param initializeTimeout the timeout value used while initializing a component
   * @return an instance of ComponentInfo
   */
  def from(
      name: String,
      componentType: ComponentType,
      prefix: Prefix,
      className: String,
      locationServiceUsage: LocationServiceUsage,
      connections: util.Set[Connection],
      initializeTimeout: Duration
  ): ComponentInfo = ComponentInfo(
    name,
    componentType,
    prefix,
    className,
    locationServiceUsage,
    connections.asScala.toSet,
    initializeTimeout.toScala
  )
}
