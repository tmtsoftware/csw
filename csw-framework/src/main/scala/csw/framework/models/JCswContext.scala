package csw.framework.models
import csw.framework.CurrentStatePublisher
import csw.command.models.framework.ComponentInfo
import csw.alarm.api.javadsl.IAlarmService
import csw.command.CommandResponseManager
import csw.config.api.javadsl.IConfigClientService
import csw.event.api.javadsl.IEventService
import csw.location.api.javadsl.ILocationService
import csw.logging.javadsl.JLoggerFactory

/**
 * Bundles all the services provided by csw, supporting java api
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
case class JCswContext(
    locationService: ILocationService,
    eventService: IEventService,
    alarmService: IAlarmService,
    loggerFactory: JLoggerFactory,
    configClientService: IConfigClientService,
    commandResponseManager: CommandResponseManager,
    currentStatePublisher: CurrentStatePublisher,
    componentInfo: ComponentInfo
) {

  /**
   * Returns the Java API for this instance of csw services
   */
  def asScala = new CswContext(
    locationService.asScala,
    eventService.asScala,
    alarmService.asScala,
    loggerFactory.asScala,
    configClientService.asScala,
    currentStatePublisher,
    commandResponseManager,
    componentInfo: ComponentInfo
  )
}
