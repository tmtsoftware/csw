package csw.common.framework.scaladsl

import akka.typed.ActorRef
import akka.typed.scaladsl.adapter._
import csw.common.framework.internal.configparser.ComponentInfoParser
import csw.common.framework.models.{ContainerMsg, SupervisorExternalMessage}
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.scaladsl.{LocationServiceFactory, RegistrationFactory}

object Component {

  def createContainer(config: com.typesafe.config.Config): ActorRef[ContainerMsg] = {
    val containerInfo     = ComponentInfoParser.parse(config)
    val system            = ClusterAwareSettings.system
    val locationService   = LocationServiceFactory.make()
    val containerBehavior = ContainerBehaviorFactory.behavior(containerInfo, locationService)
    system.spawn(containerBehavior, containerInfo.name)
  }

  def createStandalone(config: com.typesafe.config.Config): ActorRef[SupervisorExternalMessage] = {
    val componentInfo       = ComponentInfoParser.parseStandalone(config)
    val system              = ClusterAwareSettings.system
    val locationService     = LocationServiceFactory.make()
    val registrationFactory = new RegistrationFactory
    val supervisorBehavior  = SupervisorBehaviorFactory.behavior(componentInfo, locationService, registrationFactory)
    system.spawn(supervisorBehavior, componentInfo.name)
  }

}
