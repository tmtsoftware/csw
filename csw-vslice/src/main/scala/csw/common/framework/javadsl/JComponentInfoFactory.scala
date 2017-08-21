package csw.common.framework.javadsl

import csw.common.framework.models.ComponentInfo
import csw.services.location.models.ComponentType.{Assembly, HCD}
import csw.services.location.models.Connection

import scala.collection.JavaConverters.iterableAsScalaIterableConverter

object JComponentInfoFactory {

  def makeAssembly(componentName: String,
                   prefix: String,
                   componentClassName: String,
                   connections: java.util.Set[Connection]): ComponentInfo = {
    ComponentInfo(componentName, Assembly, prefix, componentClassName, connections.asScala.toSet)
  }

  def makeHcd(componentName: String, prefix: String, componentClassName: String): ComponentInfo =
    ComponentInfo(componentName, HCD, prefix, componentClassName, Set.empty)
}
