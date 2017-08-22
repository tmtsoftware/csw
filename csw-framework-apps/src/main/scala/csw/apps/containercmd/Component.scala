package csw.apps.containercmd

import akka.actor.ActorSystem
import akka.typed.ActorRef
import akka.typed.scaladsl.adapter._
import csw.common.framework.internal.configparser.ComponentInfoParser
import csw.common.framework.models.{ComponentInfo, ContainerInfo, ContainerMsg, SupervisorExternalMessage}
import csw.common.framework.scaladsl.{ContainerBehaviorFactory, SupervisorBehaviorFactory}

object Component {
  def create(
      standalone: Boolean,
      config: com.typesafe.config.Config
  ): ActorRef[SupervisorExternalMessage with ContainerMsg] = {
    if (standalone) {
      createStandalone(ComponentInfoParser.parseStandalone(config))
    } else {
      createContainer(ComponentInfoParser.parse(config))
    }
  }

  private def createStandalone(componentInfo: ComponentInfo): ActorRef[SupervisorExternalMessage] = {
    val system             = ActorSystem(s"${componentInfo.name}-system")
    val supervisorBehavior = SupervisorBehaviorFactory.behavior(componentInfo)
    system.spawn(supervisorBehavior, componentInfo.name)
  }

  private def createContainer(containerInfo: ContainerInfo): ActorRef[ContainerMsg] = {
    val system            = ActorSystem(s"${containerInfo.name}-system")
    val containerBehavior = ContainerBehaviorFactory.behavior(containerInfo)
    system.spawn(containerBehavior, containerInfo.name)
  }
}
