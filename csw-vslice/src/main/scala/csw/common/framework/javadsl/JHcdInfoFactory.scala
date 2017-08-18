package csw.common.framework.javadsl

import csw.common.framework.models.ComponentInfo.HcdInfo
import csw.common.framework.models.LocationServiceUsage

import scala.concurrent.duration.FiniteDuration

object JHcdInfoFactory {

  def make(componentName: String,
           prefix: String,
           componentClassName: String,
           locationServiceUsage: LocationServiceUsage,
           rate: FiniteDuration): HcdInfo = {
    HcdInfo(componentName, prefix, componentClassName, locationServiceUsage)
  }
}
