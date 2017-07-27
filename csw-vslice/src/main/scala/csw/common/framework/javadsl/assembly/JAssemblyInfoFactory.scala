package csw.common.framework.javadsl.assembly

import csw.common.framework.models.Component.{AssemblyInfo, LocationServiceUsage}
import csw.services.location.models.{Connection, ConnectionType}

import scala.collection.JavaConverters.asScalaSetConverter

object JAssemblyInfoFactory {

  def make(componentName: String,
           prefix: String,
           componentClassName: String,
           locationServiceUsage: LocationServiceUsage,
           registerAs: java.util.Set[ConnectionType],
           connections: java.util.Set[Connection]): AssemblyInfo = {
    AssemblyInfo(componentName,
                 prefix,
                 componentClassName,
                 locationServiceUsage,
                 registerAs.asScala.toSet,
                 connections.asScala.toSet)
  }
}
