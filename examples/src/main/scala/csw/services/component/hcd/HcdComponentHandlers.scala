package csw.services.component.hcd

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.ccs.commands.CommandResponse.Accepted
import csw.messages.ccs.commands.{CommandResponse, ControlCommand, Observe, Setup}
import csw.messages.framework.ComponentInfo
import csw.messages.location.{LocationRemoved, LocationUpdated, TrackingEvent}
import csw.messages.models.PubSub.PublisherMessage
import csw.messages.params.states.CurrentState
import csw.messages.{CommandResponseManagerMessage, TopLevelActorMessage}
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

import scala.async.Async.async
import scala.concurrent.{ExecutionContextExecutor, Future}

//#component-handlers-class
class HcdComponentHandlers(
    ctx: ActorContext[TopLevelActorMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: ActorRef[CommandResponseManagerMessage],
    pubSubRef: ActorRef[PublisherMessage[CurrentState]],
    locationService: LocationService,
    loggerFactory: LoggerFactory
) extends ComponentHandlers(
      ctx,
      componentInfo,
      commandResponseManager,
      pubSubRef,
      locationService,
      loggerFactory: LoggerFactory
    )
//#component-handlers-class
    {

  val log: Logger = new LoggerFactory(componentInfo.name).getLogger(ctx)

  implicit val ec: ExecutionContextExecutor = ctx.executionContext

  //#initialize-handler
  override def initialize(): Future[Unit] = async {

    /*
   * Initialization could include following steps :
   * 1. fetch config (preferably from configuration service)
   * 2. create a worker actor which is used by this hcd
   * 3. initialise some state by using the worker actor created above
   * */

  }
  //#initialize-handler

  //#validateCommand-handler
  override def validateCommand(controlCommand: ControlCommand): CommandResponse = controlCommand match {
    case _: Setup   ⇒ Accepted(controlCommand.runId) // validation for setup goes here
    case _: Observe ⇒ Accepted(controlCommand.runId) // validation for observe goes here
  }
  //#validateCommand-handler

  //#onSubmit-handler
  override def onSubmit(controlCommand: ControlCommand): Unit = controlCommand match {
    case setup: Setup     ⇒ submitSetup(setup)
    case observe: Observe ⇒ submitObserve(observe)
  }
  //#onSubmit-handler

  //#onOneway-handler
  override def onOneway(controlCommand: ControlCommand): Unit = controlCommand match {
    case setup: Setup     ⇒ onewaySetup(setup)
    case observe: Observe ⇒ onewayObserve(observe)
  }
  //#onOneway-handler

  //#onGoOffline-handler
  override def onGoOffline(): Unit = {
    // do something when going offline
  }
  //#onGoOffline-handler

  //#onGoOnline-handler
  override def onGoOnline(): Unit = {
    // do something when going online
  }
  //#onGoOnline-handler

  //#onShutdown-handler
  override def onShutdown(): Future[Unit] = async {
    // clean up resources
  }
  //#onShutdown-handler

  //#onLocationTrackingEvent-handler
  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = trackingEvent match {
    case LocationUpdated(location)   ⇒ // do something for the tracked location when it is updated
    case LocationRemoved(connection) ⇒ // do something for the tracked location when it is no longer available
  }
  //#onLocationTrackingEvent-handler

  private def processSetup(sc: Setup): Unit = {
    sc.commandName.toString match {
      case "axisMove"   ⇒
      case "axisDatum"  ⇒
      case "axisHome"   ⇒
      case "axisCancel" ⇒
      case x            ⇒ log.error(s"Invalid command [$x] received.")
    }
  }

  private def processObserve(oc: Observe): Unit = {
    oc.commandName.toString match {
      case "point"   ⇒
      case "acquire" ⇒
      case x         ⇒ log.error(s"Invalid command [$x] received.")
    }
  }

  /**
   * in case of submit command, component writer is required to update commandResponseManager with the result
   */
  private def submitSetup(setup: Setup): Unit = processSetup(setup)

  private def submitObserve(observe: Observe): Unit = processObserve(observe)

  private def onewaySetup(setup: Setup): Unit = processSetup(setup)

  private def onewayObserve(observe: Observe): Unit = processObserve(observe)
}
