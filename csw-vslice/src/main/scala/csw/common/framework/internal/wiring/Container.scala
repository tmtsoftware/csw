package csw.common.framework.internal.wiring

import akka.typed.ActorRef
import akka.typed.scaladsl.adapter._
import csw.common.framework.internal.configparser.ComponentInfoParser
import csw.common.framework.internal.container.ContainerBehaviorFactory
import csw.common.framework.models.ContainerMessage
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.scaladsl.LocationServiceFactory

object Container {
  def spawn(config: com.typesafe.config.Config): ActorRef[ContainerMessage] = {
    val containerInfo     = ComponentInfoParser.parse(config)
    val system            = ClusterAwareSettings.system
    val locationService   = LocationServiceFactory.make()
    val containerBehavior = ContainerBehaviorFactory.behavior(containerInfo, locationService)
    system.spawn(containerBehavior, containerInfo.name)
  }
}
