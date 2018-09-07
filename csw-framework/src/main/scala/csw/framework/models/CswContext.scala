package csw.framework.models
import csw.framework.CurrentStatePublisher
import csw.services.alarm.api.scaladsl.AlarmService
import csw.services.command.CommandResponseManager
import csw.services.event.api.scaladsl.EventService
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory

class CswContext(
    val locationService: LocationService,
    val eventService: EventService,
    val alarmService: AlarmService,
    val loggerFactory: LoggerFactory,
    val currentStatePublisher: CurrentStatePublisher,
    val commandResponseManager: CommandResponseManager
)
