package csw.common.components.command

import akka.actor.typed.scaladsl.ActorContext
import csw.common.components.command.ComponentStateForCommand._
import csw.framework.scaladsl.{ComponentHandlers, CurrentStatePublisher}
import csw.messages.commands.CommandResponse.{Accepted, Completed, Error}
import csw.messages.commands.{CommandResponse, ControlCommand}
import csw.messages.framework.ComponentInfo
import csw.messages.location.TrackingEvent
import csw.messages.scaladsl.CommandResponseManagerMessage.AddOrUpdateCommand
import csw.messages.scaladsl.TopLevelActorMessage
import csw.services.command.scaladsl.CommandResponseManager
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class McsHcdComponentHandlers(
    ctx: ActorContext[TopLevelActorMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: CommandResponseManager,
    currentStatePublisher: CurrentStatePublisher,
    locationService: LocationService,
    loggerFactory: LoggerFactory
) extends ComponentHandlers(
      ctx,
      componentInfo,
      commandResponseManager,
      currentStatePublisher,
      locationService,
      loggerFactory: LoggerFactory
    ) {

  override def initialize(): Future[Unit] = Future.unit

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = {
    controlCommand.commandName match {
      case `longRunning`               ⇒ Accepted(controlCommand.runId)
      case `mediumRunning`             ⇒ Accepted(controlCommand.runId)
      case `shortRunning`              ⇒ Accepted(controlCommand.runId)
      case `failureAfterValidationCmd` ⇒ Accepted(controlCommand.runId)
      case _                           ⇒ CommandResponse.Error(controlCommand.runId, "")
    }
  }

  override def onSubmit(controlCommand: ControlCommand): Unit = {
    controlCommand.commandName match {
      case `longRunning` ⇒
        ctx.schedule(5.seconds,
                     commandResponseManager.commandResponseManagerActor,
                     AddOrUpdateCommand(controlCommand.runId, Completed(controlCommand.runId)))
      case `mediumRunning` ⇒
        ctx.schedule(3.seconds,
                     commandResponseManager.commandResponseManagerActor,
                     AddOrUpdateCommand(controlCommand.runId, Completed(controlCommand.runId)))
      case `shortRunning` ⇒
        ctx.schedule(1.seconds,
                     commandResponseManager.commandResponseManagerActor,
                     AddOrUpdateCommand(controlCommand.runId, Completed(controlCommand.runId)))
      case `failureAfterValidationCmd` ⇒
        commandResponseManager.addOrUpdateCommand(controlCommand.runId, Error(controlCommand.runId, "Failed command"))
      case _ ⇒
    }
  }

  override def onOneway(controlCommand: ControlCommand): Unit = ???

  override def onShutdown(): Future[Unit] = ???

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???
}
