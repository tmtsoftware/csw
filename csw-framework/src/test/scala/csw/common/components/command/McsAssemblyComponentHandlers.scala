package csw.common.components.command

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.util.Timeout
import csw.common.components.command.ComponentStateForCommand._
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.ccs.commands.CommandResponse.Accepted
import csw.messages.ccs.commands.{CommandResponse, ControlCommand, Setup}
import csw.messages.framework.ComponentInfo
import csw.messages.location.{AkkaLocation, TrackingEvent}
import csw.messages.models.PubSub
import csw.messages.params.states.CurrentState
import csw.messages.{CommandResponseManagerMessage, ComponentMessage}
import csw.services.ccs.common.ActorRefExts.RichComponentActor
import csw.services.location.scaladsl.LocationService

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationDouble

class McsAssemblyComponentHandlers(
    ctx: ActorContext[ComponentMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: ActorRef[CommandResponseManagerMessage],
    pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]],
    locationService: LocationService
) extends ComponentHandlers[ComponentDomainMessage](ctx, componentInfo, commandResponseManager, pubSubRef, locationService) {

  implicit val timeout: Timeout     = 5.seconds
  implicit val scheduler: Scheduler = ctx.system.scheduler
  implicit val ec: ExecutionContext = ctx.executionContext

  override def initialize(): Future[Unit] = ???

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def onDomainMsg(msg: ComponentDomainMessage): Unit = ???

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = controlCommand.prefix match {
    case `longRunningCmdPrefix` => Accepted(controlCommand.runId)
  }

  override def onSubmit(controlCommand: ControlCommand, replyTo: ActorRef[CommandResponse]): Unit =
    locationService.resolve[AkkaLocation](componentInfo.connections.head.asInstanceOf, 5.seconds).map {
      case Some(hcd) ⇒
        hcd.componentRef().submit(Setup(controlCommand.obsId, `longRunningCmdPrefix`))
        hcd.componentRef().submit(Setup(controlCommand.obsId, `shortRunningCmdPrefix`))
        hcd.componentRef().submit(Setup(controlCommand.obsId, `mediumRunningCmdPrefix`))
      case None ⇒
    }

  override def onOneway(controlCommand: ControlCommand): Unit = ???

  override def onShutdown(): Future[Unit] = ???

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???
}
