package csw.framework.models
import csw.framework.CurrentStatePublisher
import csw.services.alarm.api.scaladsl.AlarmService
import csw.services.command.CommandResponseManager
import csw.services.event.api.scaladsl.EventService
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory

/**
 * Bundles all the services provided by csw
 *
 * @param locationService the single instance of location service
 * @param eventService the single instance of event service with default publishers and subscribers as well as the capability to create new ones
 * @param alarmService the single instance of alarm service that allows setting severity for an alarm
 * @param loggerFactory factory to create suitable logger instance
 * @param currentStatePublisher the pub sub actor to publish state represented by [[csw.messages.params.states.CurrentState]] for this component
 * @param commandResponseManager manages state of a received Submit command
 *
 */
class CswServices(
    val locationService: LocationService,
    val eventService: EventService,
    val alarmService: AlarmService,
    val loggerFactory: LoggerFactory,
    val currentStatePublisher: CurrentStatePublisher,
    val commandResponseManager: CommandResponseManager
)
