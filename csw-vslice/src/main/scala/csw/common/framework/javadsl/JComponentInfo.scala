package csw.common.framework.javadsl

import java.util

import csw.common.framework.models.ComponentInfo
import csw.services.location.models.{ComponentType, Connection}

import scala.collection.JavaConverters.iterableAsScalaIterableConverter

object JComponentInfo {

  def from(
      name: String,
      componentType: ComponentType,
      prefix: String,
      className: String,
      connections: util.Set[Connection]
  ): ComponentInfo = {
    ComponentInfo(name, componentType, prefix, className, connections.asScala.toSet)
  }
}
