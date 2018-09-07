package csw.framework.models
import csw.framework.CurrentStatePublisher
import csw.services.alarm.api.javadsl.IAlarmService
import csw.services.command.CommandResponseManager
import csw.services.config.api.javadsl.IConfigClientService
import csw.services.event.api.javadsl.IEventService
import csw.services.location.javadsl.ILocationService
import csw.services.logging.javadsl.JLoggerFactory

/**
 * Bundles all the services provided by csw, supporting java api
 *
 * @param locationService the single instance of location service
 * @param eventService the single instance of event service with default publishers and subscribers as well as the capability to create new ones
 * @param alarmService the single instance of alarm service that allows setting severity for an alarm
 * @param loggerFactory factory to create suitable logger instance
 * @param currentStatePublisher the pub sub actor to publish state represented by [[csw.messages.params.states.CurrentState]] for this component
 * @param commandResponseManager manages state of a received Submit command
 *
 */
case class JCswServices(
    locationService: ILocationService,
    eventService: IEventService,
    alarmService: IAlarmService,
    loggerFactory: JLoggerFactory,
    configClientService: IConfigClientService,
    commandResponseManager: CommandResponseManager,
    currentStatePublisher: CurrentStatePublisher
) {

  /**
   * Returns the Java API for this instance of csw services
   */
  def asScala = new CswServices(
    locationService.asScala,
    eventService.asScala,
    alarmService.asScala,
    loggerFactory.asScala,
    configClientService.asScala,
    currentStatePublisher,
    commandResponseManager
  )
}
