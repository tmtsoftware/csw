package csw.common.components.framework

import akka.typed.scaladsl.ActorContext
import csw.framework.scaladsl.{ComponentHandlers, CurrentStatePublisher}
import csw.messages.TopLevelActorMessage
import csw.messages.ccs.CommandIssue.OtherIssue
import csw.messages.ccs.commands.CommandResponse.{Accepted, Completed, Invalid}
import csw.messages.ccs.commands._
import csw.messages.framework.ComponentInfo
import csw.messages.location.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.messages.location.{LocationRemoved, LocationUpdated, TrackingEvent}
import csw.messages.params.states.CurrentState
import csw.services.ccs.scaladsl.CommandResponseManager
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

import scala.concurrent.Future

class SampleComponentHandlers(
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

  val log: Logger = new LoggerFactory(componentInfo.name).getLogger(ctx)

  import SampleComponentState._

  override def initialize(): Future[Unit] = {
    // DEOPSCSW-153: Accessibility of logging service to other CSW components
    log.info("Initializing Component TLA")
    Thread.sleep(100)
    currentStatePublisher.publish(CurrentState(prefix, Set(choiceKey.set(initChoice))))

    // DEOPSCSW-219: Discover component connection using HTTP protocol
    trackConnection(httpConnection)
    trackConnection(tcpConnection)
    Future.unit
  }

  override def onGoOffline(): Unit = currentStatePublisher.publish(CurrentState(prefix, Set(choiceKey.set(offlineChoice))))

  override def onGoOnline(): Unit = currentStatePublisher.publish(CurrentState(prefix, Set(choiceKey.set(onlineChoice))))

  override def onSubmit(controlCommand: ControlCommand): Unit = {
    // Adding passed in parameter to see if data is transferred properly
    commandResponseManager.addOrUpdateCommand(controlCommand.runId, Completed(controlCommand.runId))
    currentStatePublisher.publish(CurrentState(prefix, Set(choiceKey.set(submitCommandChoice))))
    processCommand(controlCommand)
  }

  override def onOneway(controlCommand: ControlCommand): Unit = {
    // Adding passed in parameter to see if data is transferred properly
    currentStatePublisher.publish(CurrentState(prefix, Set(choiceKey.set(oneWayCommandChoice))))
    processCommand(controlCommand)
  }

  private def processCommand(controlCommand: ControlCommand): Unit =
    controlCommand match {
      case Setup(_, somePrefix, _, _, _) ⇒
        currentStatePublisher.publish(CurrentState(somePrefix, controlCommand.paramSet + choiceKey.set(setupConfigChoice)))
      case Observe(_, somePrefix, _, _, _) ⇒
        currentStatePublisher.publish(CurrentState(somePrefix, controlCommand.paramSet + choiceKey.set(observeConfigChoice)))
      case _ ⇒
    }

  def validateCommand(command: ControlCommand): CommandResponse = {
    currentStatePublisher.publish(CurrentState(prefix, Set(choiceKey.set(commandValidationChoice))))
    if (command.commandName.name.contains("success")) Accepted(command.runId)
    else Invalid(command.runId, OtherIssue("Testing: Received failure, will return Invalid."))
  }

  override def onShutdown(): Future[Unit] = {
    currentStatePublisher.publish(CurrentState(prefix, Set(choiceKey.set(shutdownChoice))))
    Thread.sleep(100)
    Future.unit
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = trackingEvent match {
    case LocationUpdated(location) ⇒
      location.connection match {
        case _: AkkaConnection =>
          currentStatePublisher.publish(CurrentState(prefix, Set(choiceKey.set(akkaLocationUpdatedChoice))))
        case _: HttpConnection =>
          currentStatePublisher.publish(CurrentState(prefix, Set(choiceKey.set(httpLocationUpdatedChoice))))
        case _: TcpConnection =>
          currentStatePublisher.publish(CurrentState(prefix, Set(choiceKey.set(tcpLocationUpdatedChoice))))
      }
    case LocationRemoved(connection) ⇒
      connection match {
        case _: AkkaConnection =>
          currentStatePublisher.publish(CurrentState(prefix, Set(choiceKey.set(akkaLocationRemovedChoice))))
        case _: HttpConnection =>
          currentStatePublisher.publish(CurrentState(prefix, Set(choiceKey.set(httpLocationRemovedChoice))))
        case _: TcpConnection =>
          currentStatePublisher.publish(CurrentState(prefix, Set(choiceKey.set(tcpLocationRemovedChoice))))
      }
  }
}
