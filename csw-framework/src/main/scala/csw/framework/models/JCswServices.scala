package csw.framework.models
import csw.framework.CurrentStatePublisher
import csw.services.alarm.api.javadsl.IAlarmService
import csw.services.command.CommandResponseManager
import csw.services.event.api.javadsl.IEventService
import csw.services.location.javadsl.ILocationService
import csw.services.logging.javadsl.JLoggerFactory

case class JCswServices(
    locationService: ILocationService,
    eventService: IEventService,
    alarmService: IAlarmService,
    loggerFactory: JLoggerFactory,
    commandResponseManager: CommandResponseManager,
    currentStatePublisher: CurrentStatePublisher
) {
  def asScala = new CswServices(
    locationService.asScala,
    eventService.asScala,
    alarmService.asScala,
    loggerFactory.asScala,
    currentStatePublisher,
    commandResponseManager
  )
}
