package csw.framework.javadsl

import java.util

import csw.messages.framework.{ComponentInfo, LocationServiceUsage}
import csw.messages.location.{ComponentType, Connection}

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.concurrent.duration.FiniteDuration

/**
 * Java API for creating [[csw.messages.framework.ComponentInfo]]
 */
object JComponentInfo {

  def from(
      name: String,
      componentType: ComponentType,
      prefix: String,
      className: String,
      locationServiceUsage: LocationServiceUsage,
      connections: util.Set[Connection],
      initializeTimeout: FiniteDuration
  ): ComponentInfo = {
    ComponentInfo(
      name,
      componentType,
      prefix,
      className,
      locationServiceUsage,
      connections.asScala.toSet,
      initializeTimeout
    )
  }
}
