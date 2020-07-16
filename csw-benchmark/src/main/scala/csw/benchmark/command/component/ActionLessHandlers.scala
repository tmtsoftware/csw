package csw.benchmark.command.component

import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.{ComponentContext, CswContext}
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.TrackingEvent
import csw.logging.api.scaladsl.Logger
import csw.params.commands.CommandResponse.{Accepted, Completed, SubmitResponse, ValidateCommandResponse}
import csw.params.commands._
import csw.params.core.models.Id
import csw.time.core.models.UTCTime

import scala.concurrent.Future

class ActionLessHandlers(ctx: ComponentContext[TopLevelActorMessage], cswCtx: CswContext) extends ComponentHandlers(ctx, cswCtx) {

  val log: Logger = cswCtx.loggerFactory.getLogger(ctx.self)

  override def initialize(): Future[Unit] = Future.unit

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def validateCommand(runId: Id, controlCommand: ControlCommand): ValidateCommandResponse = Accepted(runId)

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse = Completed(runId)

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = ???

  override def onShutdown(): Future[Unit] = ???

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = ???

  override def onOperationsMode(): Unit = ???
}
