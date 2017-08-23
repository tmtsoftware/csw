package csw.common.framework.scaladsl

import akka.actor.ActorSystem
import akka.typed.ActorRef
import akka.typed.scaladsl.adapter._
import csw.common.framework.internal.configparser.ComponentInfoParser
import csw.common.framework.models.{ContainerMsg, SupervisorExternalMessage}
import csw.services.location.scaladsl.LocationServiceFactory

object Component {

  def createContainer(config: com.typesafe.config.Config): ActorRef[ContainerMsg] = {
    val containerInfo     = ComponentInfoParser.parse(config)
    val system            = ActorSystem(s"${containerInfo.name}-system")
    val locationService   = LocationServiceFactory.withSystem(system)
    val containerBehavior = ContainerBehaviorFactory.behavior(containerInfo, locationService)
    system.spawn(containerBehavior, containerInfo.name)
  }

  def createStandalone(config: com.typesafe.config.Config): ActorRef[SupervisorExternalMessage] = {
    val componentInfo      = ComponentInfoParser.parseStandalone(config)
    val system             = ActorSystem(s"${componentInfo.name}-system")
    val supervisorBehavior = SupervisorBehaviorFactory.behavior(componentInfo)
    system.spawn(supervisorBehavior, componentInfo.name)
  }

}
