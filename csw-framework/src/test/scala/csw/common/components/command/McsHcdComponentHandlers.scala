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
import csw.params.core.models.Id
import csw.time.core.models.UTCTime

import scala.concurrent.Future

class McsHcdComponentHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends ComponentHandlers(ctx, cswCtx) {

  import cswCtx._
  override def initialize(): Future[Unit] = Future.unit

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {}

  override def validateCommand(runId: Id, controlCommand: ControlCommand): ValidateCommandResponse = {
    controlCommand.commandName match {
      case `longRunning`               => Accepted(runId)
      case `mediumRunning`             => Accepted(runId)
      case `shortRunning`              => Accepted(runId)
      case `failureAfterValidationCmd` => Accepted(runId)
      case _                           => Invalid(runId, UnsupportedCommandIssue(controlCommand.commandName.name))
    }
  }

  //#updateCommand
  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse = {
    controlCommand.commandName match {
      case `longRunning` =>
        timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plusSeconds(5))) {
          commandResponseManager.updateCommand(Completed(runId))
        }
        Started(runId)
      //#updateCommand
      case `mediumRunning` =>
        timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plusSeconds(3))) {
          commandResponseManager.updateCommand(Completed(runId))
        }
        Started(runId)
      case `shortRunning` =>
        timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plusSeconds(1))) {
          commandResponseManager.updateCommand(Completed(runId))
          //commandResponseManager.updateCommand(Error(runId, "Totally fucked"))
        }
        Started(runId)
      case `failureAfterValidationCmd` =>
        Error(runId, "Failed command")
      case _ =>
        Error(runId, "Unknown Command")
    }
  }

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {}

  override def onShutdown(): Future[Unit] = Future.unit

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = {}

  override def onOperationsMode(): Unit = {}

  override def onGoOffline(): Unit = {}

  override def onGoOnline(): Unit = {}
}
