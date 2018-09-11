package csw.services.command.perf.component

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.CswServices
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.TopLevelActorMessage
import csw.messages.commands.CommandResponse.Completed
import csw.messages.commands._
import csw.services.location.api.models.TrackingEvent
import csw.messages.params.models.Id
import csw.services.logging.scaladsl.Logger

import scala.concurrent.Future

class ActionLessHandlers(ctx: ActorContext[TopLevelActorMessage], cswServices: CswServices)
    extends ComponentHandlers(ctx, cswServices) {

  val log: Logger = cswServices.loggerFactory.getLogger(ctx)

  override def initialize(): Future[Unit] = Future.successful(Unit)

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = Completed(Id())

  override def onSubmit(controlCommand: ControlCommand): Unit = ???

  override def onOneway(controlCommand: ControlCommand): Unit = ???

  override def onShutdown(): Future[Unit] = ???

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???
}
