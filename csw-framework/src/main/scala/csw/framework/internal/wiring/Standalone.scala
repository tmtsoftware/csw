package csw.framework.internal.wiring


import csw.framework.internal.configparser.ConfigParser
import csw.framework.internal.supervisor.SupervisorBehaviorFactory
import csw.messages.ActorTypes.ComponentRef

import scala.concurrent.Future

/**
 * Start a supervisor actor without a container, in it's own actor system, using the component information provided in a configuration file
 */
object Standalone {

  def spawn(
      config: com.typesafe.config.Config,
      wiring: FrameworkWiring
  ): Future[ComponentRef] = {
    import wiring._
    val componentInfo = ConfigParser.parseStandalone(config)
    val supervisorBehavior = SupervisorBehaviorFactory.make(
      None,
      componentInfo,
      locationService,
      registrationFactory,
      pubSubBehaviorFactory
    )
    val cswFrameworkSystem = new CswFrameworkSystem(actorSystem)
    cswFrameworkSystem.spawnTyped(supervisorBehavior, componentInfo.name)
  }
}
