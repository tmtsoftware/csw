package csw.common.components.command

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.framework.scaladsl.ComponentHandlers
import csw.messages._
import csw.messages.ccs.CommandIssue.{OtherIssue, WrongPrefixIssue}
import csw.messages.ccs.commands.CommandResponse.{Accepted, Completed, Invalid}
import csw.messages.ccs.commands._
import csw.messages.framework.ComponentInfo
import csw.messages.location._
import csw.messages.models.PubSub.PublisherMessage
import csw.messages.params.states.CurrentState
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.FrameworkLogger

import scala.concurrent.Future

class ComponentHandlerForCommand(
    ctx: ActorContext[ComponentMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: ActorRef[CommandResponseManagerMessage],
    pubSubRef: ActorRef[PublisherMessage[CurrentState]],
    locationService: LocationService
) extends ComponentHandlers[ComponentDomainMessage](ctx, componentInfo, commandResponseManager, pubSubRef, locationService)
    with FrameworkLogger.Simple {

  import ComponentStateForCommand._

  override protected def componentName(): String = "ComponentHandlerForCommand"

  override def initialize(): Future[Unit] = Future.unit

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def onDomainMsg(msg: ComponentDomainMessage): Unit = ???

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = controlCommand.prefix match {
    case `acceptedCmdPrefix`  ⇒ Accepted(controlCommand.runId)
    case `immediateCmdPrefix` ⇒ Completed(controlCommand.runId)
    case `invalidCmdPrefix`   ⇒ Invalid(controlCommand.runId, OtherIssue(s"Unsupported prefix: ${controlCommand.prefix.prefix}"))
    case _                    ⇒ Invalid(controlCommand.runId, WrongPrefixIssue(s"Wrong prefix: ${controlCommand.prefix.prefix}"))
  }

  override def onSubmit(controlCommand: ControlCommand, replyTo: ActorRef[CommandResponse]): Unit = ???

  override def onOneway(controlCommand: ControlCommand): Unit = ???

  override def onShutdown(): Future[Unit] = ???

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???
}
