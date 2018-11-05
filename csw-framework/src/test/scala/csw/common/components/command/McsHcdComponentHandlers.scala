package csw.common.components.command

import akka.actor.typed.scaladsl.ActorContext
import csw.common.components.command.ComponentStateForCommand._
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.command.client.messages.CommandResponseManagerMessage.AddOrUpdateCommand
import csw.command.client.messages.TopLevelActorMessage
import csw.params.commands.CommandResponse._
import csw.params.commands.ControlCommand
import csw.location.api.models.TrackingEvent
import csw.params.commands.CommandIssue.UnsupportedCommandIssue

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class McsHcdComponentHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends ComponentHandlers(ctx, cswCtx) {

  import cswCtx._
  override def initialize(): Future[Unit] = Future.unit

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def validateCommand(controlCommand: ControlCommand): ValidateCommandResponse = {
    controlCommand.commandName match {
      case `longRunning`               ⇒ Accepted(controlCommand.runId)
      case `mediumRunning`             ⇒ Accepted(controlCommand.runId)
      case `shortRunning`              ⇒ Accepted(controlCommand.runId)
      case `failureAfterValidationCmd` ⇒ Accepted(controlCommand.runId)
      case _ ⇒
        Invalid(controlCommand.runId, UnsupportedCommandIssue(controlCommand.commandName.name))
    }
  }

  //#addOrUpdateCommand
  override def onSubmit(controlCommand: ControlCommand): SubmitResponse = {
    controlCommand.commandName match {
      case `longRunning` ⇒
        ctx.scheduleOnce(5.seconds,
                         commandResponseManager.commandResponseManagerActor,
                         AddOrUpdateCommand(Completed(controlCommand.runId)))
        Started(controlCommand.runId)
      //#addOrUpdateCommand
      case `mediumRunning` ⇒
        ctx.scheduleOnce(3.seconds,
                         commandResponseManager.commandResponseManagerActor,
                         AddOrUpdateCommand(Completed(controlCommand.runId)))
        Started(controlCommand.runId)
      case `shortRunning` ⇒
        ctx.scheduleOnce(1.seconds,
                         commandResponseManager.commandResponseManagerActor,
                         AddOrUpdateCommand(Completed(controlCommand.runId)))
        Started(controlCommand.runId)
      case `failureAfterValidationCmd` ⇒
        commandResponseManager.addOrUpdateCommand(Error(controlCommand.runId, "Failed command"))
        Error(controlCommand.runId, "Failed command")
      case _ ⇒
        Error(controlCommand.runId, "Unknown Command")
    }
  }

  override def onOneway(controlCommand: ControlCommand): Unit = ???

  override def onShutdown(): Future[Unit] = ???

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???
}
