package csw.common.components.command

import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.common.components.command.ComponentStateForCommand._
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.models.TrackingEvent
import csw.params.commands.CommandIssue.UnsupportedCommandIssue
import csw.params.commands.CommandResponse._
import csw.params.commands.ControlCommand
import csw.time.core.models.UTCTime
import csw.params.core.models.Id

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class McsHcdComponentHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends ComponentHandlers(ctx, cswCtx) {

  import cswCtx._
  override def initialize(): Future[Unit] = Future.unit

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {}

  override def validateCommand(runId: Id, controlCommand: ControlCommand): ValidateCommandResponse = {
    controlCommand.commandName match {
      case `longRunning`               => Accepted(controlCommand.commandName, runId)
      case `mediumRunning`             => Accepted(controlCommand.commandName, runId)
      case `shortRunning`              => Accepted(controlCommand.commandName, runId)
      case `failureAfterValidationCmd` => Accepted(controlCommand.commandName, runId)
      case _ =>
        Invalid(controlCommand.commandName, runId, UnsupportedCommandIssue(controlCommand.commandName.name))
    }
  }

  //#addOrUpdateCommand
  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse = {
    import ctx.executionContext
    val scheduler = ctx.system.scheduler

    new Runnable {
      override def run(): Unit = {}
    }

    controlCommand.commandName match {
      case `longRunning` =>
        scheduler.scheduleOnce(
          5.seconds,
          () => commandResponseManager.updateCommand(Completed(controlCommand.commandName, runId))
        )
        Started(controlCommand.commandName, runId)
      case `mediumRunning` =>
        scheduler.scheduleOnce(
          3.seconds,
          () => commandResponseManager.updateCommand(Completed(controlCommand.commandName, runId))
        )
        Started(controlCommand.commandName, runId)
      case `shortRunning` =>
        scheduler.scheduleOnce(
          1.seconds,
          () => commandResponseManager.updateCommand(Completed(controlCommand.commandName, runId))
        )
        Started(controlCommand.commandName, runId)
      case `failureAfterValidationCmd` =>
        //  SHOULDN"T BE NEEDED commandUpdatePublisher.update(Error(controlCommand.commandName, runId, "Failed command"))
        Error(controlCommand.commandName, runId, "Failed command")
      case _ =>
        Error(controlCommand.commandName, runId, "Unknown Command")
    }
  }

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {}

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = ???

  override def onOperationsMode(): Unit = ???

  override def onShutdown(): Future[Unit] = Future.unit

  override def onGoOffline(): Unit = {}

  override def onGoOnline(): Unit = {}
}
