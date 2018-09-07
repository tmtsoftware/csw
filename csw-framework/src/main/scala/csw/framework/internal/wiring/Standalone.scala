package csw.framework.internal.wiring

import akka.actor.typed.ActorRef
import csw.framework.CurrentStatePublisher
import csw.framework.internal.configparser.ConfigParser
import csw.framework.internal.pubsub.PubSubBehaviorFactory
import csw.framework.internal.supervisor.SupervisorBehaviorFactory
import csw.framework.models.CswContext
import csw.messages.ComponentMessage
import csw.messages.params.states.CurrentState
import csw.services.logging.scaladsl.LoggerFactory

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
  def spawn(
      config: com.typesafe.config.Config,
      wiring: FrameworkWiring
  ): Future[ActorRef[ComponentMessage]] = {
    import wiring._
    import actorRuntime._

    val componentInfo                   = ConfigParser.parseStandalone(config)
    val richSystem                      = new CswFrameworkSystem(system)
    val PubSubComponentActor            = "pub-sub-component"
    val CommandResponseManagerActorName = "command-response-manager"

    async {
      val eventService  = eventServiceFactory.make(locationService)
      val alarmService  = alarmServiceFactory.makeClientApi(locationService)
      val loggerFactory = new LoggerFactory(componentInfo.name)
      val pubSubComponentActor = await(
        richSystem.spawnTyped(new PubSubBehaviorFactory().make[CurrentState](PubSubComponentActor, loggerFactory),
                              PubSubComponentActor)
      )
      val currentStatePublisher = new CurrentStatePublisher(pubSubComponentActor)

      val commandResponseManagerActor =
        await(richSystem.spawnTyped(commandResponseManagerFactory.makeBehavior(loggerFactory), CommandResponseManagerActorName))
      val commandResponseManager = commandResponseManagerFactory.make(commandResponseManagerActor)

      val cswCtx = new CswContext(
        locationService,
        eventService,
        alarmService,
        loggerFactory,
        currentStatePublisher,
        commandResponseManager
      )

      val supervisorBehavior = SupervisorBehaviorFactory.make(None, componentInfo, registrationFactory, cswCtx)
      await(richSystem.spawnTyped(supervisorBehavior, componentInfo.name))
    }
  }
}
