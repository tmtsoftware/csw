package csw.framework.scaladsl

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.framework.models._
import csw.param.messages.PubSub.PublisherMessage
import csw.param.messages.RunningMessage.DomainMessage
import csw.param.messages.{CommandExecutionResponse, CommandMessage, CommandValidationResponse, ComponentMessage}
import csw.param.models.Validation
import csw.param.models.location.TrackingEvent
import csw.param.states.CurrentState
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
  def onRun(): Future[Unit]
  def onDomainMsg(msg: Msg): Unit
  def onControlCommand(commandMessage: CommandMessage): Validation
  def onCommandValidationNotification(validationResponse: CommandValidationResponse): Unit
  def onCommandExecutionNotification(executionResponse: CommandExecutionResponse): Unit
  def onShutdown(): Future[Unit]
  def onGoOffline(): Unit
  def onGoOnline(): Unit
}
