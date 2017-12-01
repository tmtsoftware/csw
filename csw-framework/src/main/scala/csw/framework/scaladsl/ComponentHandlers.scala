package csw.framework.scaladsl

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.messages.CommonMessage.TrackingEventReceived
import csw.messages.RunningMessage.DomainMessage
import csw.messages.ccs.commands.{CommandResponse, ControlCommand}
import csw.messages.framework.ComponentInfo
import csw.messages.location.{Connection, TrackingEvent}
import csw.messages.models.PubSub.PublisherMessage
import csw.messages.params.states.CurrentState
import csw.messages.{CommandResponseManagerMessage, ComponentMessage}
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
 * Base class for component handlers which will be used by the component actor
 * @param ctx               The Actor Context under which the actor instance of the component, which use these handlers, is created
 * @param componentInfo     Component related information as described in the configuration file
 * @param pubSubRef         The pub sub actor to publish state represented by [[csw.messages.params.states.CurrentState]] for this component
 * @param locationService   The single instance of Location service created for a running application
 * @tparam Msg              The type of messages created for domain specific message hierarchy of any component
 */
abstract class ComponentHandlers[Msg <: DomainMessage: ClassTag](
    ctx: ActorContext[ComponentMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: ActorRef[CommandResponseManagerMessage],
    pubSubRef: ActorRef[PublisherMessage[CurrentState]],
    locationService: LocationService,
    loggerFactory: LoggerFactory
) {
  var isOnline: Boolean = false

  def initialize(): Future[Unit]
  def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit
  def onDomainMsg(msg: Msg): Unit
  def validateCommand(controlCommand: ControlCommand): CommandResponse
  def onSubmit(controlCommand: ControlCommand, replyTo: ActorRef[CommandResponse]): Unit
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
