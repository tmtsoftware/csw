package csw.common.framework.internal.wiring

import akka.typed.{ActorRef, Behavior}
import com.typesafe.config.Config
import csw.common.framework.internal.configparser.ComponentInfoParser
import csw.common.framework.internal.container.ContainerBehaviorFactory
import csw.common.framework.internal.extensions.RichSystemExtension.RichSystem
import csw.common.framework.models.ContainerMessage

import scala.concurrent.Future

object Container {
  def spawn(config: Config, wiring: FrameworkWiring): Future[ActorRef[ContainerMessage]] = {
    import wiring._
    val containerInfo = ComponentInfoParser.parseContainer(config)
    val containerBehavior: Behavior[ContainerMessage] =
      ContainerBehaviorFactory.behavior(containerInfo, locationService)
    val richSystem = new RichSystem(actorSystem)
    richSystem.spawnTyped(containerBehavior, containerInfo.name)
  }
}
