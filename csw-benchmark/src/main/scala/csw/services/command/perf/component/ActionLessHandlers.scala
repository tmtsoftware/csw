package csw.services.command.perf.component

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.CurrentStatePublisher
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.TopLevelActorMessage
import csw.messages.commands.CommandResponse.Completed
import csw.messages.commands._
import csw.messages.framework.ComponentInfo
import csw.messages.location.TrackingEvent
import csw.messages.params.models.Id
import csw.services.alarm.api.scaladsl.AlarmService
import csw.services.command.CommandResponseManager
import csw.services.event.api.scaladsl.EventService
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

import scala.concurrent.Future

class ActionLessHandlers(
    ctx: ActorContext[TopLevelActorMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: CommandResponseManager,
    currentStatePublisher: CurrentStatePublisher,
    locationService: LocationService,
    eventService: EventService,
    alarmService: AlarmService,
    loggerFactory: LoggerFactory
) extends ComponentHandlers(
      ctx,
      componentInfo,
      commandResponseManager,
      currentStatePublisher,
      locationService,
      eventService,
      loggerFactory: LoggerFactory
    ) {

  val log: Logger = new LoggerFactory(componentInfo.name).getLogger(ctx)

  override def initialize(): Future[Unit] = Future.successful(Unit)

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = Completed(Id())

  override def onSubmit(controlCommand: ControlCommand): Unit = ???

  override def onOneway(controlCommand: ControlCommand): Unit = ???

  override def onShutdown(): Future[Unit] = ???

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???
}
