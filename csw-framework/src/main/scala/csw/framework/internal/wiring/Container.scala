package csw.framework.internal.wiring

import akka.typed.{ActorRef, Behavior}
import com.typesafe.config.Config
import csw.framework.internal.configparser.ComponentInfoParser
import csw.framework.internal.container.ContainerBehaviorFactory
import csw.framework.internal.extensions.RichSystemExtension.RichSystem
import csw.framework.models.ContainerMessage

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
