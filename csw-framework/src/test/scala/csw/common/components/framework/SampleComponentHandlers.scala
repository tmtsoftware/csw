package csw.common.components.framework

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.ccs.CommandIssue.OtherIssue
import csw.messages.ccs.commands.CommandResponse.{Accepted, Invalid}
import csw.messages.ccs.commands.{CommandResponse, ControlCommand, Observe, Setup}
import csw.messages.framework.ComponentInfo
import csw.messages.location.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.messages.location.{LocationRemoved, LocationUpdated, TrackingEvent}
import csw.messages.models.PubSub.{Publish, PublisherMessage}
import csw.messages.params.states.CurrentState
import csw.messages.{CommandResponseManagerMessage, ComponentMessage}
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

import scala.concurrent.Future

class SampleComponentHandlers(
    ctx: ActorContext[ComponentMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: ActorRef[CommandResponseManagerMessage],
    pubSubRef: ActorRef[PublisherMessage[CurrentState]],
    locationService: LocationService
) extends ComponentHandlers[ComponentDomainMessage](ctx, componentInfo, commandResponseManager, pubSubRef, locationService) {
  val log: Logger = new LoggerFactory(componentInfo.name).getLogger

  import SampleComponentState._

  override def initialize(): Future[Unit] = {
    // DEOPSCSW-153: Accessibility of logging service to other CSW components
    log.info("Initializing Component TLA")
    Thread.sleep(100)
    pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(initChoice))))

    // DEOPSCSW-219: Discover component connection using HTTP protocol
    trackConnection(httpConnection)
    trackConnection(tcpConnection)
    Future.unit
  }

  override def onGoOffline(): Unit = pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(offlineChoice))))

  override def onGoOnline(): Unit = pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(onlineChoice))))

  override def onDomainMsg(msg: ComponentDomainMessage): Unit = {
    pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(domainChoice))))
  }

  override def onSubmit(controlCommand: ControlCommand, replyTo: ActorRef[CommandResponse]): Unit = {
    // Adding passed in parameter to see if data is transferred properly
    pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(submitCommandChoice))))
    processCommand(controlCommand)
  }

  override def onOneway(controlCommand: ControlCommand): Unit = {
    // Adding passed in parameter to see if data is transferred properly
    pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(oneWayCommandChoice))))
    processCommand(controlCommand)
  }

  private def processCommand(controlCommand: ControlCommand): Unit =
    controlCommand match {
      case Setup(_, _, somePrefix, _) ⇒
        pubSubRef ! Publish(CurrentState(somePrefix, controlCommand.paramSet + choiceKey.set(setupConfigChoice)))
      case Observe(_, _, somePrefix, _) ⇒
        pubSubRef ! Publish(CurrentState(somePrefix, controlCommand.paramSet + choiceKey.set(observeConfigChoice)))
      case _ ⇒
    }

  def validateCommand(command: ControlCommand): CommandResponse = {
    pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(commandValidationChoice))))
    if (command.prefix.prefix.contains("success")) Accepted(command.runId)
    else Invalid(command.runId, OtherIssue("Testing: Received failure, will return Invalid."))
  }

  override def onShutdown(): Future[Unit] = {
    pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(shutdownChoice))))
    Thread.sleep(100)
    Future.unit
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = trackingEvent match {
    case LocationUpdated(location) ⇒
      location.connection match {
        case _: AkkaConnection =>
          pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(akkaLocationUpdatedChoice))))
        case _: HttpConnection =>
          pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(httpLocationUpdatedChoice))))
        case _: TcpConnection =>
          pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(tcpLocationUpdatedChoice))))
      }
    case LocationRemoved(connection) ⇒
      connection match {
        case _: AkkaConnection =>
          pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(akkaLocationRemovedChoice))))
        case _: HttpConnection =>
          pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(httpLocationRemovedChoice))))
        case _: TcpConnection =>
          pubSubRef ! Publish(CurrentState(prefix, Set(choiceKey.set(tcpLocationRemovedChoice))))
      }
  }
}
