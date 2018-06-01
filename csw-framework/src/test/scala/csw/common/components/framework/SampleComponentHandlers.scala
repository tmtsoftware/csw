package csw.common.components.framework

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.scaladsl.{ComponentHandlers, CurrentStatePublisher}
import csw.messages.commands.CommandIssue.OtherIssue
import csw.messages.commands.CommandResponse.{Accepted, Completed, Invalid}
import csw.messages.commands._
import csw.messages.framework.ComponentInfo
import csw.messages.location.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.messages.location.{LocationRemoved, LocationUpdated, TrackingEvent}
import csw.messages.params.states.{CurrentState, StateName}
import csw.messages.scaladsl.TopLevelActorMessage
import csw.services.command.scaladsl.CommandResponseManager
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

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

  val log: Logger                   = new LoggerFactory(componentInfo.name).getLogger(ctx)
  implicit val ec: ExecutionContext = ctx.executionContext

  import SampleComponentState._

  override def initialize(): Future[Unit] = {
    // DEOPSCSW-153: Accessibility of logging service to other CSW components
    log.info("Initializing Component TLA")
    Thread.sleep(100)

    //#currentStatePublisher
    // Publish the CurrentState using parameter set created using a sample Choice parameter
    currentStatePublisher.publish(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))
    //#currentStatePublisher

    // DEOPSCSW-219: Discover component connection using HTTP protocol
    trackConnection(httpConnection)
    trackConnection(tcpConnection)
    Future.unit
  }

  override def onGoOffline(): Unit =
    currentStatePublisher.publish(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(offlineChoice))))

  override def onGoOnline(): Unit =
    currentStatePublisher.publish(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(onlineChoice))))

  override def onSubmit(controlCommand: ControlCommand): Unit = {
    // Adding passed in parameter to see if data is transferred properly
    commandResponseManager.addOrUpdateCommand(controlCommand.runId, Completed(controlCommand.runId))
    currentStatePublisher.publish(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(submitCommandChoice))))
    processCommand(controlCommand)
  }

  override def onOneway(controlCommand: ControlCommand): Unit = {
    // Adding passed in parameter to see if data is transferred properly
    currentStatePublisher.publish(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(oneWayCommandChoice))))
    processCommand(controlCommand)
  }

  // DEOPSCSW-372: Provide an API for PubSubActor that hides actor based interaction
  private def processCommand(controlCommand: ControlCommand): Unit =
    controlCommand match {
      case Setup(_, somePrefix, _, _, _) ⇒
        currentStatePublisher.publish(
          CurrentState(somePrefix, StateName("testStateName"), controlCommand.paramSet + choiceKey.set(setupConfigChoice))
        )
      case Observe(_, somePrefix, _, _, _) ⇒
        currentStatePublisher.publish(
          CurrentState(somePrefix, StateName("testStateName"), controlCommand.paramSet + choiceKey.set(observeConfigChoice))
        )
      case _ ⇒
    }

  def validateCommand(command: ControlCommand): CommandResponse = {
    currentStatePublisher.publish(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(commandValidationChoice))))
    if (command.commandName.name.contains("success")) Accepted(command.runId)
    else Invalid(command.runId, OtherIssue("Testing: Received failure, will return Invalid."))
  }

  override def onShutdown(): Future[Unit] = {
    currentStatePublisher.publish(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice))))
    Thread.sleep(100)
    Future.unit
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = trackingEvent match {
    case LocationUpdated(location) ⇒
      location.connection match {
        case _: AkkaConnection =>
          Future {
            Thread.sleep(100)
            currentStatePublisher.publish(
              CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(akkaLocationUpdatedChoice)))
            )
          }
        case _: HttpConnection =>
          currentStatePublisher.publish(
            CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(httpLocationUpdatedChoice)))
          )
        case _: TcpConnection =>
          currentStatePublisher.publish(
            CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(tcpLocationUpdatedChoice)))
          )
      }
    case LocationRemoved(connection) ⇒
      connection match {
        case _: AkkaConnection =>
          currentStatePublisher.publish(
            CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(akkaLocationRemovedChoice)))
          )
        case _: HttpConnection =>
          currentStatePublisher.publish(
            CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(httpLocationRemovedChoice)))
          )
        case _: TcpConnection =>
          currentStatePublisher.publish(
            CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(tcpLocationRemovedChoice)))
          )
      }
  }
}
