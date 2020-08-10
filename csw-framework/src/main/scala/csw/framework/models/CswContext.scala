package csw.framework.models
import akka.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import csw.alarm.api.scaladsl.AlarmService
import csw.alarm.client.AlarmServiceFactory
import csw.command.client.{CommandResponseManager, MiniCRM}
import csw.command.client.models.framework.ComponentInfo
import csw.config.api.scaladsl.ConfigClientService
import csw.config.client.scaladsl.ConfigClientFactory
import csw.event.api.scaladsl.EventService
import csw.event.client.EventServiceFactory
import csw.framework.CurrentStatePublisher
import csw.framework.internal.pubsub.PubSubBehavior
import csw.framework.internal.wiring.CswFrameworkSystem
import csw.location.api.scaladsl.LocationService
import csw.logging.client.scaladsl.LoggerFactory
import csw.params.core.states.CurrentState
import csw.time.scheduler.TimeServiceSchedulerFactory
import csw.time.scheduler.api.TimeServiceScheduler

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * Bundles all the services provided by csw
 *
 * @param locationService       the single instance of location service
 * @param eventService          the single instance of event service with default publishers and subscribers as well as the capability to create new ones
 * @param alarmService          the single instance of alarm service that allows setting severity for an alarm
 * @param loggerFactory         factory to create suitable logger instance
 * @param currentStatePublisher the pub sub actor to publish state represented by [[csw.params.core.states.CurrentState]] for this component
 * @param commandResponseManager manages state of a started Submit command
 * @param componentInfo         component related information as described in the configuration file
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

    implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = richSystem.system
    implicit val scheduler: Scheduler                            = richSystem.scheduler
    implicit val ec: ExecutionContextExecutor                    = typedSystem.executionContext

    val eventService         = eventServiceFactory.make(locationService)
    val alarmService         = alarmServiceFactory.makeClientApi(locationService)
    val timeServiceScheduler = new TimeServiceSchedulerFactory().make()

    val loggerFactory       = new LoggerFactory(componentInfo.prefix)
    val configClientService = ConfigClientFactory.clientApi(typedSystem, locationService)
    async {

      // create CurrentStatePublisher
      val pubSubComponentActor =
        await(richSystem.spawnTyped(PubSubBehavior.make[CurrentState](loggerFactory), PubSubComponentActor))
      val currentStatePublisher = new CurrentStatePublisher(pubSubComponentActor)

      // create CommandResponseManager (CRM)
      val crmBehavior = MiniCRM.make()
      val crmActor    = await(richSystem.spawnTyped(crmBehavior, CommandResponseManagerActorName))
      val crm         = new CommandResponseManager(crmActor)

      new CswContext(
        locationService,
        eventService,
        alarmService,
        timeServiceScheduler,
        loggerFactory,
        configClientService,
        currentStatePublisher,
        crm,
        componentInfo
      )
    }
  }
}
