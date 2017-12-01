package csw.common.components.command

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.util.Timeout
import csw.common.components.command.ComponentDomainMessage.CommandCompleted
import csw.common.components.command.ComponentStateForCommand._
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.CommandResponseManagerMessage.AddOrUpdateCommand
import csw.messages.ccs.commands.CommandResponse.{Accepted, Completed}
import csw.messages.ccs.commands.{CommandResponse, ControlCommand, Setup}
import csw.messages.framework.ComponentInfo
import csw.messages.location.{AkkaLocation, TrackingEvent}
import csw.messages.models.PubSub
import csw.messages.params.models.RunId
import csw.messages.params.states.CurrentState
import csw.messages.{CommandResponseManagerMessage, ComponentMessage, SupervisorExternalMessage}
import csw.services.ccs.common.ActorRefExts.RichComponentActor
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

class McsAssemblyComponentHandlers(
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
      loggerFactory
    ) {

  implicit val timeout: Timeout                   = 10.seconds
  implicit val scheduler: Scheduler               = ctx.system.scheduler
  implicit val ec: ExecutionContext               = ctx.executionContext
  var completedCommands: Int                      = 0
  var hcdRef: ActorRef[SupervisorExternalMessage] = _
  var commandId: RunId                            = _

  override def initialize(): Future[Unit] =
    Future.successful(componentInfo.connections.headOption match {
      case Some(hcd) ⇒
        locationService.resolve(hcd.of[AkkaLocation], 5.seconds).map {
          case Some(akkaLocation) ⇒ hcdRef = akkaLocation.componentRef()
          case None               ⇒
        }
      case None ⇒
    })

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = Unit

  override def onDomainMsg(msg: ComponentDomainMessage): Unit = msg match {
    case CommandCompleted(_) ⇒
      completedCommands += 1
      if (completedCommands == 3) commandResponseManager ! AddOrUpdateCommand(commandId, Completed(commandId))
    case _ ⇒
  }

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = {
    controlCommand.prefix match {
      case `longRunningCmdPrefix` ⇒ Accepted(controlCommand.runId)
      case _                      ⇒ CommandResponse.Error(controlCommand.runId, "")
    }
  }

  override def onSubmit(controlCommand: ControlCommand, replyTo: ActorRef[CommandResponse]): Unit = {
    commandId = controlCommand.runId
    processCommand(Setup(controlCommand.obsId, `shortRunningCmdPrefix`))
    processCommand(Setup(controlCommand.obsId, `mediumRunningCmdPrefix`))
    processCommand(Setup(controlCommand.obsId, `longRunningCmdPrefix`))
  }

  private def processCommand(controlCommand: ControlCommand) = {
    hcdRef
      .submit(controlCommand)
      .map {
        case _: Accepted ⇒
          hcdRef.getCommandResponse(controlCommand.runId).map {
            case _: Completed ⇒ ctx.self ! CommandCompleted(Completed(controlCommand.runId))
            case _            ⇒ // Do nothing
          }
        case _ ⇒ // Do nothing
      }
  }

  override def onOneway(controlCommand: ControlCommand): Unit = ???

  override def onShutdown(): Future[Unit] = ???

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???
}
