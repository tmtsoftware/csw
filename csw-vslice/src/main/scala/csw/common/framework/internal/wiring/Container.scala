package csw.common.framework.internal.wiring

import akka.typed.scaladsl.adapter._
import akka.typed.{ActorRef, Behavior}
import com.typesafe.config.Config
import csw.common.framework.internal.configparser.ComponentInfoParser
import csw.common.framework.internal.container.ContainerBehaviorFactory
import csw.common.framework.models.ContainerMessage

object Container {
  def spawn(config: Config, wiring: FrameworkWiring): ActorRef[ContainerMessage] = {
    import wiring._
    val containerInfo = ComponentInfoParser.parse(config)
    val containerBehavior: Behavior[ContainerMessage] =
      ContainerBehaviorFactory.behavior(containerInfo, locationService)
    actorSystem.spawn(containerBehavior, containerInfo.name)
  }
}
