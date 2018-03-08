package csw.framework.scaladsl

import akka.typed.scaladsl.ActorContext
import csw.messages.commands.{CommandResponse, ControlCommand}
import csw.messages.framework.ComponentInfo
import csw.messages.location.{Connection, TrackingEvent}
import csw.messages.scaladsl.TopLevelActorCommonMessage.TrackingEventReceived
import csw.messages.scaladsl.TopLevelActorMessage
import csw.services.command.scaladsl.CommandResponseManager
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory

import scala.concurrent.Future

/**
 * Base class for component handlers which will be used by the component actor
 *
 * @param ctx                     The Actor Context under which the actor instance of the component, which use these handlers, is created
 * @param componentInfo           Component related information as described in the configuration file
 * @param currentStatePublisher   The pub sub actor to publish state represented by [[csw.messages.params.states.CurrentState]] for this component
 * @param locationService         The single instance of Location service created for a running application
 */
abstract class ComponentHandlers(
    ctx: ActorContext[TopLevelActorMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: CommandResponseManager,
    currentStatePublisher: CurrentStatePublisher,
    locationService: LocationService,
    loggerFactory: LoggerFactory
) {
  var isOnline: Boolean = false

  def initialize(): Future[Unit]
  def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit
  def validateCommand(controlCommand: ControlCommand): CommandResponse
  def onSubmit(controlCommand: ControlCommand): Unit
  def onOneway(controlCommand: ControlCommand): Unit
  def onShutdown(): Future[Unit]
  def onGoOffline(): Unit
  def onGoOnline(): Unit

  /**
   * Track any connection. The handlers for received events are defined in onLocationTrackingEvent() method
   * @param connection Connection to be tracked for location updates
   */
  def trackConnection(connection: Connection): Unit =
    locationService.subscribe(connection, trackingEvent ⇒ ctx.self ! TrackingEventReceived(trackingEvent))
}
