package csw.apps.clusterseed.components

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.PubSub.PublisherMessage
import csw.messages.RunningMessage.DomainMessage
import csw.messages._
import csw.messages.ccs.Validation
import csw.messages.ccs.Validations.Valid
import csw.messages.ccs.commands.ControlCommand
import csw.messages.framework.ComponentInfo
import csw.messages.location.TrackingEvent
import csw.messages.params.states.CurrentState
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.ComponentLogger

import scala.concurrent.Future

case class StartLogging() extends DomainMessage

class GalilComponentHandlers(
    ctx: ActorContext[ComponentMessage],
    componentInfo: ComponentInfo,
    pubSubRef: ActorRef[PublisherMessage[CurrentState]],
    locationService: LocationService
) extends ComponentHandlers[StartLogging](ctx, componentInfo, pubSubRef, locationService)
    with ComponentLogger.Simple {

  override def initialize(): Future[Unit] = Future.successful(())

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ()

  override def onDomainMsg(msg: StartLogging): Unit = {
    log.trace("Level is trace")
    log.debug("Level is debug")
    log.info("Level is info")
    log.warn("Level is warn")
    log.error("Level is error")
    log.fatal("Level is fatal")
  }

  override def onSetup(commandMessage: CommandMessage): Validation = Valid

  override def onObserve(commandMessage: CommandMessage): Validation = Valid

  override def onSubmit(controlCommand: ControlCommand, replyTo: ActorRef[CommandResponse]): Validation = Valid
  override def onOneway(controlCommand: ControlCommand): Validation = Valid

  override def onShutdown(): Future[Unit] = Future.successful(())

  override def onGoOffline(): Unit = ()

  override def onGoOnline(): Unit = ()

  override protected def componentName(): String = componentInfo.name
}
