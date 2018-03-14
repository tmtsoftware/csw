package csw.framework.internal.wiring

import akka.typed.{ActorRef, Behavior}
import com.typesafe.config.Config
import csw.framework.internal.configparser.ConfigParser
import csw.framework.internal.container.ContainerBehaviorFactory
import csw.messages.scaladsl.{ContainerActorMessage, ContainerMessage}

import scala.concurrent.Future

/**
 * Start a container actor in it's own actor system, using the container information provided in a configuration file
 */
//TODO: add doc for significance
private[csw] object Container {
  def spawn(config: Config, wiring: FrameworkWiring): Future[ActorRef[ContainerMessage]] = {
    import wiring._
    val containerInfo = ConfigParser.parseContainer(config)
    val containerBehavior: Behavior[ContainerActorMessage] =
      ContainerBehaviorFactory.behavior(containerInfo, locationService, registrationFactory)
    val cswFrameworkSystem = new CswFrameworkSystem(actorSystem)
    cswFrameworkSystem.spawnTyped(containerBehavior, containerInfo.name)
  }
}
