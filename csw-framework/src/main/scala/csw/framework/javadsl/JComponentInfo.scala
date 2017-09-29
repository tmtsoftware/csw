package csw.framework.javadsl

import java.util

import csw.framework.models.{ComponentInfo, LocationServiceUsage}
import csw.param.models.location.{ComponentType, Connection}

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.concurrent.duration.FiniteDuration

object JComponentInfo {

  def from(
      name: String,
      componentType: ComponentType,
      prefix: String,
      className: String,
      locationServiceUsage: LocationServiceUsage,
      connections: util.Set[Connection],
      initializeTimeout: FiniteDuration,
      runTimeout: FiniteDuration
  ): ComponentInfo = {
    ComponentInfo(
      name,
      componentType,
      prefix,
      className,
      locationServiceUsage,
      connections.asScala.toSet,
      initializeTimeout,
      runTimeout
    )
  }
}
