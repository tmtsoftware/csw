package csw.common.components.command

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.components.command.ComponentDomainMessage.{LongCommandCompleted, MediumCommandCompleted, ShortCommandCompleted}
import csw.common.components.command.ComponentStateForCommand._
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.CommandResponseManagerMessage.AddOrUpdateCommand
import csw.messages.ccs.commands.CommandResponse.{Accepted, Completed}
import csw.messages.ccs.commands.{CommandResponse, ControlCommand}
import csw.messages.framework.ComponentInfo
import csw.messages.location.TrackingEvent
import csw.messages.models.PubSub
import csw.messages.params.states.CurrentState
import csw.messages.{CommandResponseManagerMessage, ComponentMessage}
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class McsHcdComponentHandlers(
    ctx: ActorContext[ComponentMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: ActorRef[CommandResponseManagerMessage],
    pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]],
    locationService: LocationService,
    loggerFactory: LoggerFactory
) extends ComponentHandlers[ComponentDomainMessage](
      ctx,
      componentInfo,
      commandResponseManager,
      pubSubRef,
      locationService,
      loggerFactory: LoggerFactory
    ) {

  override def initialize(): Future[Unit] = Future.unit

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def onDomainMsg(msg: ComponentDomainMessage): Unit = msg match {
    case LongCommandCompleted(commandResponse) =>
      commandResponseManager ! AddOrUpdateCommand(commandResponse.runId, commandResponse)
    case MediumCommandCompleted(commandResponse) =>
      commandResponseManager ! AddOrUpdateCommand(commandResponse.runId, commandResponse)
    case ShortCommandCompleted(commandResponse) =>
      commandResponseManager ! AddOrUpdateCommand(commandResponse.runId, commandResponse)
    case _ ⇒
  }

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = {
    controlCommand.prefix match {
      case `longRunningCmdPrefix`   ⇒ Accepted(controlCommand.runId)
      case `mediumRunningCmdPrefix` ⇒ Accepted(controlCommand.runId)
      case `shortRunningCmdPrefix`  ⇒ Accepted(controlCommand.runId)
      case _                        ⇒ CommandResponse.Error(controlCommand.runId, "")
    }
  }

  override def onSubmit(controlCommand: ControlCommand): Unit = {
    controlCommand.prefix match {
      case `longRunningCmdPrefix` ⇒
        ctx.schedule(5.seconds, ctx.self, LongCommandCompleted(Completed(controlCommand.runId)))
      case `mediumRunningCmdPrefix` ⇒
        ctx.schedule(3.seconds, ctx.self, MediumCommandCompleted(Completed(controlCommand.runId)))
      case `shortRunningCmdPrefix` ⇒
        ctx.schedule(1.seconds, ctx.self, ShortCommandCompleted(Completed(controlCommand.runId)))
      case _ ⇒
    }
  }

  override def onOneway(controlCommand: ControlCommand): Unit = ???

  override def onShutdown(): Future[Unit] = ???

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???
}
