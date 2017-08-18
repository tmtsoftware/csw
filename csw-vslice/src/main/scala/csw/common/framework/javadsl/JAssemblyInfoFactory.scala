package csw.common.framework.javadsl

import csw.common.framework.models.ComponentInfo.AssemblyInfo
import csw.common.framework.models.LocationServiceUsage
import csw.services.location.models.Connection

import scala.collection.JavaConverters.asScalaSetConverter

object JAssemblyInfoFactory {

  def make(componentName: String,
           prefix: String,
           componentClassName: String,
           locationServiceUsage: LocationServiceUsage,
           connections: java.util.Set[Connection]): AssemblyInfo = {
    AssemblyInfo(componentName, prefix, componentClassName, locationServiceUsage, connections.asScala.toSet)
  }

}
