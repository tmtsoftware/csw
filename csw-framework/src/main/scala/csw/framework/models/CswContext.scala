package csw.framework.models
import akka.actor.ActorSystem
import csw.framework.CurrentStatePublisher
import csw.framework.internal.pubsub.PubSubBehaviorFactory
import csw.framework.internal.wiring.CswFrameworkSystem
import csw.command.client.models.framework.ComponentInfo
import csw.location.api.scaladsl.LocationService
import csw.params.core.states.CurrentState
import csw.alarm.api.scaladsl.AlarmService
import csw.alarm.client.AlarmServiceFactory
import csw.command.client.CommandResponseManager
import csw.command.client.internal.CommandResponseManagerFactory
import csw.config.api.scaladsl.ConfigClientService
import csw.config.client.scaladsl.ConfigClientFactory
import csw.event.client.EventServiceFactory
import csw.event.api.scaladsl.EventService
import csw.logging.client.scaladsl.LoggerFactory
import csw.time.client.TimeServiceSchedulerFactory
import csw.time.client.api.TimeServiceScheduler

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * Bundles all the services provided by csw
 *
 * @param locationService the single instance of location service
 * @param eventService the single instance of event service with default publishers and subscribers as well as the capability to create new ones
 * @param alarmService the single instance of alarm service that allows setting severity for an alarm
 * @param loggerFactory factory to create suitable logger instance
 * @param currentStatePublisher the pub sub actor to publish state represented by [[csw.params.core.states.CurrentState]] for this component
 * @param commandResponseManager manages state of a received Submit command
 * @param componentInfo component related information as described in the configuration file
 *
 */
class CswContext(
    val locationService: LocationService,
    val eventService: EventService,
    val alarmService: AlarmService,
    val timeServiceScheduler: TimeServiceScheduler,
    val loggerFactory: LoggerFactory,
    val configClientService: ConfigClientService,
    val currentStatePublisher: CurrentStatePublisher,
    val commandResponseManager: CommandResponseManager,
    val componentInfo: ComponentInfo
)

object CswContext {

  private val PubSubComponentActor            = "pub-sub-component"
  private val CommandResponseManagerActorName = "command-response-manager"

  private[framework] def make(
      locationService: LocationService,
      eventServiceFactory: EventServiceFactory,
      alarmServiceFactory: AlarmServiceFactory,
      componentInfo: ComponentInfo
  )(implicit richSystem: CswFrameworkSystem): Future[CswContext] = {

    implicit val system: ActorSystem          = richSystem.system
    implicit val ec: ExecutionContextExecutor = system.dispatcher

    val eventService         = eventServiceFactory.make(locationService)
    val alarmService         = alarmServiceFactory.makeClientApi(locationService)
    val timeServiceScheduler = TimeServiceSchedulerFactory.make()

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

      new CswContext(
        locationService,
        eventService,
        alarmService,
        timeServiceScheduler,
        loggerFactory,
        configClientService,
        currentStatePublisher,
        commandResponseManager,
        componentInfo
      )
    }
  }
}
