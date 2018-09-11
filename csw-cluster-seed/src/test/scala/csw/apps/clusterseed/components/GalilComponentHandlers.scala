package csw.apps.clusterseed.components

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.CswServices
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.TopLevelActorMessage
import csw.messages.commands.CommandResponse.Accepted
import csw.messages.commands.{CommandResponse, ControlCommand}
import csw.services.location.api.models.TrackingEvent
import csw.services.logging.scaladsl.Logger

import scala.concurrent.Future

case class StartLogging()

class GalilComponentHandlers(ctx: ActorContext[TopLevelActorMessage], cswServices: CswServices)
    extends ComponentHandlers(ctx, cswServices) {

  val log: Logger = cswServices.loggerFactory.getLogger

  override def initialize(): Future[Unit] = Future.successful(())

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ()

  private def startLogging(): Unit = {
    log.trace("Level is trace")
    log.debug("Level is debug")
    log.info("Level is info")
    log.warn("Level is warn")
    log.error("Level is error")
    log.fatal("Level is fatal")
  }

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = Accepted(controlCommand.runId)

  override def onSubmit(controlCommand: ControlCommand): Unit = ()

  override def onOneway(controlCommand: ControlCommand): Unit =
    if (controlCommand.commandName.name == "StartLogging") startLogging()

  override def onShutdown(): Future[Unit] = Future.successful(())

  override def onGoOffline(): Unit = ()

  override def onGoOnline(): Unit = ()
}
