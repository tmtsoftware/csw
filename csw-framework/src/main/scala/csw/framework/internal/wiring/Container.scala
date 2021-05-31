package csw.framework.internal.wiring

import akka.actor.typed.{ActorRef, Behavior}
import com.typesafe.config.Config
import csw.command.client.messages.{ContainerActorMessage, ContainerMessage}
import csw.framework.internal.configparser.ConfigParser
import csw.framework.internal.container.ContainerBehaviorFactory
import csw.prefix.models.Prefix

import scala.concurrent.Future

/**
 * Start a container actor in it's own actor system, using the container information provided in a configuration file
 */
private[csw] object Container {
  def spawn(config: Config, wiring: FrameworkWiring, agentPrefix: Option[Prefix] = None): Future[ActorRef[ContainerMessage]] = {
    import wiring._
    val containerInfo = ConfigParser.parseContainer(config)
    val containerBehavior: Behavior[ContainerActorMessage] =
      ContainerBehaviorFactory.behavior(
        containerInfo,
        locationService,
        eventServiceFactory,
        alarmServiceFactory,
        registrationFactory,
        agentPrefix
      )
    val cswFrameworkSystem = new CswFrameworkSystem(actorSystem)
    cswFrameworkSystem.spawnTyped(containerBehavior, containerInfo.prefix.toString)
  }
}
