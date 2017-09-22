package csw.framework.javadsl

import java.util

import csw.framework.models.{ComponentInfo, LocationServiceUsage}
import csw.services.location.models.{ComponentType, Connection}

import scala.collection.JavaConverters.iterableAsScalaIterableConverter

object JComponentInfo {

  def from(
      name: String,
      componentType: ComponentType,
      prefix: String,
      className: String,
      locationServiceUsage: LocationServiceUsage,
      connections: util.Set[Connection],
      initializeTimeoutInSeconds: Int,
      runTimeoutInSeconds: Int
  ): ComponentInfo = {
    ComponentInfo(
      name,
      componentType,
      prefix,
      className,
      locationServiceUsage,
      connections.asScala.toSet,
      initializeTimeoutInSeconds,
      runTimeoutInSeconds
    )
  }
}
