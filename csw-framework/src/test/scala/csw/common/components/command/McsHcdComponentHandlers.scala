package csw.common.components.command

import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.command.client.models.framework.PubSub.Publish
import csw.common.components.command.ComponentStateForCommand._
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.models.TrackingEvent
import csw.params.commands.CommandIssue.UnsupportedCommandIssue
import csw.params.commands.CommandResponse._
import csw.params.commands.ControlCommand
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
    controlCommand.commandName match {
      case `longRunning` =>
        ctx.scheduleOnce(
          5.seconds,
          // Here the Completed is sent directly to the publish actor
          commandUpdatePublisher.publisherActor,
          Publish[SubmitResponse](Completed(controlCommand.commandName, runId))
        )
        Started(controlCommand.commandName, runId)
      //#addOrUpdateCommand
      case `mediumRunning` =>
        ctx.scheduleOnce(
          3.seconds,
          commandUpdatePublisher.publisherActor,
          Publish[SubmitResponse](Completed(controlCommand.commandName, runId))
        )
        Started(controlCommand.commandName, runId)
      case `shortRunning` =>
        ctx.scheduleOnce(
          1.seconds,
          commandUpdatePublisher.publisherActor,
          Publish[SubmitResponse](Completed(controlCommand.commandName, runId))
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

  override def onShutdown(): Future[Unit] = Future.unit

  override def onGoOffline(): Unit = {}

  override def onGoOnline(): Unit = {}
}
