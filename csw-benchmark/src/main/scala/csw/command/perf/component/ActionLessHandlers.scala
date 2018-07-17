package csw.command.perf.component

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.command.messages.TopLevelActorMessage
import csw.params.commands.CommandResponse.Completed
import csw.params.commands._
import csw.location.api.models.TrackingEvent
import csw.params.core.models.Id
import csw.logging.scaladsl.Logger
import csw.messages.TopLevelActorMessage
import csw.messages.commands.ValidationResponse.Accepted
import csw.messages.commands._
import csw.messages.location.TrackingEvent
import csw.messages.params.models.Id
import csw.services.logging.scaladsl.Logger

import scala.concurrent.Future

class ActionLessHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext) extends ComponentHandlers(ctx, cswCtx) {

  val log: Logger = cswCtx.loggerFactory.getLogger(ctx)

  override def initialize(): Future[Unit] = Future.successful(Unit)

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def validateCommand(controlCommand: ControlCommand): ValidationResponse = Accepted(Id())

  override def onSubmit(controlCommand: ControlCommand): CommandResponse = ???

  override def onOneway(controlCommand: ControlCommand): Unit = ???

  override def onShutdown(): Future[Unit] = ???

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???
}
