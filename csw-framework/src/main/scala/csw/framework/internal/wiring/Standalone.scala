package csw.framework.internal.wiring

import akka.typed.ActorRef
import csw.framework.internal.configparser.ConfigParser
import csw.framework.internal.supervisor.SupervisorBehaviorFactory
import csw.messages.scaladsl.ComponentMessage

import scala.concurrent.Future

/**
 * Start a supervisor actor without a container, in it's own actor system, using the component information provided in a configuration file
 */
private[csw] object Standalone {

  def spawn(
      config: com.typesafe.config.Config,
      wiring: FrameworkWiring
  ): Future[ActorRef[ComponentMessage]] = {
    import wiring._
    val componentInfo = ConfigParser.parseStandalone(config)
    val supervisorBehavior = SupervisorBehaviorFactory.make(
      None,
      componentInfo,
      locationService,
      registrationFactory,
      pubSubBehaviorFactory,
      commandResponseManagerFactory
    )
    val cswFrameworkSystem = new CswFrameworkSystem(actorSystem)
    cswFrameworkSystem.spawnTyped(supervisorBehavior, componentInfo.name)
  }
}
