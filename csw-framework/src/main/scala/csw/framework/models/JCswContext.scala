package csw.framework.models
import csw.services.alarm.api.javadsl.IAlarmService
import csw.services.event.api.javadsl.IEventService
import csw.services.location.javadsl.ILocationService
import csw.services.logging.javadsl.JLoggerFactory

case class JCswContext(
    locationService: ILocationService,
    eventService: IEventService,
    alarmService: IAlarmService,
    loggerFactory: JLoggerFactory
) {
  def asScala = CswContext(
    locationService.asScala,
    eventService.asScala,
    alarmService.asScala,
    loggerFactory.asScala
  )
}
