package csw.framework.internal.wiring

import akka.actor.typed.ActorRef
import csw.command.client.messages.ComponentMessage
import csw.framework.internal.configparser.ConfigParser
import csw.framework.internal.supervisor.SupervisorBehaviorFactory
import csw.framework.models.CswContext

import scala.async.Async.{async, await}
import scala.concurrent.Future

/**
 * Start a supervisor actor without a container, in it's own actor system, using the component information provided in a configuration file
 */
object Standalone {

  /**
   * Spawns a component in standalone mode
   *
   * @param config represents the componentInfo data
   * @param wiring represents the class for initializing necessary instances to run a component(s)
   * @return a Future that completes with actor ref of spawned component
   */
  def spawn(config: com.typesafe.config.Config, wiring: FrameworkWiring): Future[ActorRef[ComponentMessage]] = {
    import wiring._
    import actorRuntime._

    val componentInfo = ConfigParser.parseStandalone(config)
    val richSystem    = new CswFrameworkSystem(system)
    async {
      val cswCtxF            = CswContext.make(locationService, eventServiceFactory, alarmServiceFactory, componentInfo)(richSystem)
      val supervisorBehavior = SupervisorBehaviorFactory.make(None, registrationFactory, await(cswCtxF))
      await(richSystem.spawnTyped(supervisorBehavior, componentInfo.name))
    }
  }
}
