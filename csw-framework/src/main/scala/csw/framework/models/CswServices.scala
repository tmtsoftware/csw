package csw.framework.models
import akka.actor.ActorSystem
import csw.framework.CurrentStatePublisher
import csw.framework.internal.pubsub.PubSubBehaviorFactory
import csw.framework.internal.wiring.CswFrameworkSystem
import csw.messages.framework.ComponentInfo
import csw.services.location.api.scaladsl.LocationService
import csw.messages.params.states.CurrentState
import csw.services.alarm.api.scaladsl.AlarmService
import csw.services.alarm.client.AlarmServiceFactory
import csw.services.command.CommandResponseManager
import csw.services.command.internal.CommandResponseManagerFactory
import csw.services.config.api.scaladsl.ConfigClientService
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.event.EventServiceFactory
import csw.services.event.api.scaladsl.EventService
import csw.services.logging.scaladsl.LoggerFactory

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * Bundles all the services provided by csw
 *
 * @param locationService the single instance of location service
 * @param eventService the single instance of event service with default publishers and subscribers as well as the capability to create new ones
 * @param alarmService the single instance of alarm service that allows setting severity for an alarm
 * @param loggerFactory factory to create suitable logger instance
 * @param currentStatePublisher the pub sub actor to publish state represented by [[csw.messages.params.states.CurrentState]] for this component
 * @param commandResponseManager manages state of a received Submit command
 * @param componentInfo component related information as described in the configuration file
 *
 */
class CswServices(
    val locationService: LocationService,
    val eventService: EventService,
    val alarmService: AlarmService,
    val loggerFactory: LoggerFactory,
    val configClientService: ConfigClientService,
    val currentStatePublisher: CurrentStatePublisher,
    val commandResponseManager: CommandResponseManager,
    val componentInfo: ComponentInfo
)

object CswServices {

  private val PubSubComponentActor            = "pub-sub-component"
  private val CommandResponseManagerActorName = "command-response-manager"

  private[framework] def make(
      locationService: LocationService,
      eventServiceFactory: EventServiceFactory,
      alarmServiceFactory: AlarmServiceFactory,
      componentInfo: ComponentInfo
  )(implicit richSystem: CswFrameworkSystem): Future[CswServices] = {

    implicit val system: ActorSystem          = richSystem.system
    implicit val ec: ExecutionContextExecutor = system.dispatcher

    val eventService        = eventServiceFactory.make(locationService)
    val alarmService        = alarmServiceFactory.makeClientApi(locationService)
    val loggerFactory       = new LoggerFactory(componentInfo.name)
    val configClientService = ConfigClientFactory.clientApi(system, locationService)
    async {

      // create CurrentStatePublisher
      val pubSubComponentActor = await(
        richSystem.spawnTyped(new PubSubBehaviorFactory().make[CurrentState](PubSubComponentActor, loggerFactory),
                              PubSubComponentActor)
      )
      val currentStatePublisher = new CurrentStatePublisher(pubSubComponentActor)

      // create CommandResponseManager
      val commandResponseManagerFactory = new CommandResponseManagerFactory
      val commandResponseManagerActor =
        await(richSystem.spawnTyped(commandResponseManagerFactory.makeBehavior(loggerFactory), CommandResponseManagerActorName))
      val commandResponseManager = commandResponseManagerFactory.make(commandResponseManagerActor)

      new CswServices(
        locationService,
        eventService,
        alarmService,
        loggerFactory,
        configClientService,
        currentStatePublisher,
        commandResponseManager,
        componentInfo
      )
    }
  }
}
