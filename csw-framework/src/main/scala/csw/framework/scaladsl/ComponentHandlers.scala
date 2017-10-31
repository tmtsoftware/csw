package csw.framework.scaladsl

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.messages.CommonMessage.TrackingEventReceived
import csw.messages.ComponentMessage
import csw.messages.PubSub.PublisherMessage
import csw.messages.RunningMessage.DomainMessage
import csw.messages.ccs.commands.{CommandResponse, CommandValidationResponse, ControlCommand}
import csw.messages.framework.ComponentInfo
import csw.messages.location.{Connection, TrackingEvent}
import csw.messages.params.states.CurrentState
import csw.services.location.scaladsl.LocationService

import scala.concurrent.Future
import scala.reflect.ClassTag

abstract class ComponentHandlers[Msg <: DomainMessage: ClassTag](
    ctx: ActorContext[ComponentMessage],
    componentInfo: ComponentInfo,
    pubSubRef: ActorRef[PublisherMessage[CurrentState]],
    locationService: LocationService
) {
  var isOnline: Boolean = false

  def initialize(): Future[Unit]
  def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit
  def onDomainMsg(msg: Msg): Unit
  def onSubmit(controlCommand: ControlCommand, replyTo: ActorRef[CommandResponse]): CommandValidationResponse
  def onOneway(controlCommand: ControlCommand): CommandValidationResponse
  def onShutdown(): Future[Unit]
  def onGoOffline(): Unit
  def onGoOnline(): Unit
  def trackConnection(connection: Connection): Unit =
    locationService.subscribe(connection, trackingEvent ⇒ ctx.self ! TrackingEventReceived(trackingEvent))
}
