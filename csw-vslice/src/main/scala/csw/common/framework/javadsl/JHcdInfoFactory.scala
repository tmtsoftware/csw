package csw.common.framework.javadsl

import csw.common.framework.models.Component.{HcdInfo, LocationServiceUsage}
import csw.services.location.models.ConnectionType
import scala.collection.JavaConverters.asScalaSetConverter
import scala.concurrent.duration.FiniteDuration

object JHcdInfoFactory {

  def make(componentName: String,
           prefix: String,
           componentClassName: String,
           locationServiceUsage: LocationServiceUsage,
           registerAs: java.util.Set[ConnectionType],
           rate: FiniteDuration): HcdInfo = {
    HcdInfo(componentName, prefix, componentClassName, locationServiceUsage, registerAs.asScala.toSet, rate)
  }
}
