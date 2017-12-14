package csw.trombone.assembly

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import com.typesafe.config.ConfigFactory
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.CommandMessage.Submit
import csw.messages._
import csw.messages.ccs.commands.CommandResponse.Accepted
import csw.messages.ccs.commands._
import csw.messages.framework.ComponentInfo
import csw.messages.location._
import csw.messages.models.PubSub.PublisherMessage
import csw.messages.params.states.CurrentState
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory
import csw.trombone.assembly.AssemblyCommandHandlerMsgs.CommandMessageE
import csw.trombone.assembly.CommonMsgs.UpdateHcdLocations
import csw.trombone.assembly.DiagPublisherMessages.{CommandResponseE, DiagnosticState, OperationsState}
import csw.trombone.assembly.ParamValidation._
import csw.trombone.assembly.actors.{DiagPublisher, TromboneAssemblyCommandBehaviorFactory, TrombonePublisher}

import scala.async.Async.{async, await}
import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, Future}

//#component-factory
class TromboneAssemblyBehaviorFactory extends ComponentBehaviorFactory[DiagPublisherMessages] {
  override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      commandResponseManager: ActorRef[CommandResponseManagerMessage],
      pubSubRef: ActorRef[PublisherMessage[CurrentState]],
      locationService: LocationService,
      loggerFactory: LoggerFactory
  ): ComponentHandlers[DiagPublisherMessages] =
    new TromboneAssemblyHandlers(ctx, componentInfo, commandResponseManager, pubSubRef, locationService, loggerFactory)
}
//#component-factory

//#component-handlers-class
class TromboneAssemblyHandlers(
    ctx: ActorContext[TopLevelActorMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: ActorRef[CommandResponseManagerMessage],
    pubSubRef: ActorRef[PublisherMessage[CurrentState]],
    locationService: LocationService,
    loggerFactory: LoggerFactory
) extends ComponentHandlers[DiagPublisherMessages](
      ctx,
      componentInfo,
      commandResponseManager,
      pubSubRef,
      locationService,
      loggerFactory
    ) {
  //#component-handlers-class

  //private state of this component
  private var diagPublisher: ActorRef[DiagPublisherMessages]       = _
  private var commandHandler: ActorRef[AssemblyCommandHandlerMsgs] = _
  private var runningHcds: Map[Connection, Option[ComponentRef]]   = Map.empty

  private val commandResponseAdapter: ActorRef[CommandResponse] = ctx.spawnAdapter(CommandResponseE)

  implicit var ac: AssemblyContext  = _
  implicit val ec: ExecutionContext = ctx.executionContext

  def onRun(): Future[Unit] = Future.unit

  //#initialize-handler
  def initialize(): Future[Unit] = async {
    // fetch config (preferably from configuration service)
    val (calculationConfig, controlConfig) = await(getAssemblyConfigs)
    ac = AssemblyContext(componentInfo, calculationConfig, controlConfig)

    // create a worker actor which is used by this assembly
    val eventPublisher = ctx.spawnAnonymous(TrombonePublisher.make(ac))

    // find a Hcd connection from the connections provided in componentInfo
    val maybeConnection =
      componentInfo.connections.find(connection ⇒ connection.componentId.componentType == ComponentType.HCD)

    // If an Hcd is found as a connection, resolve its location from location service and create other
    // required worker actors required by this assembly
    maybeConnection match {
      case hcdConnection @ Some(hcd) ⇒
        locationService.resolve(hcd.of[AkkaLocation], 5.seconds).map {
          case Some(akkaLocation) ⇒
            runningHcds = runningHcds.updated(hcdConnection.get, Some(akkaLocation.component))
            diagPublisher = ctx.spawnAnonymous(DiagPublisher.make(ac, runningHcds(maybeConnection.get), Some(eventPublisher)))
            commandHandler =
              ctx.spawnAnonymous(new TromboneAssemblyCommandBehaviorFactory().make(ac, runningHcds, Some(eventPublisher)))
          case None ⇒ throw new RuntimeException("Could not resolve hcd location, Initialization failure.")
        }
      case None ⇒ Future.successful(Unit)
    }

  }
  //#initialize-handler

  override def onShutdown(): Future[Unit] = {
    Future.successful(println("Received Shutdown"))
  }

  override def onGoOffline(): Unit = println("Received running offline")

  override def onGoOnline(): Unit = println("Received GoOnline")

  def onDomainMsg(mode: DiagPublisherMessages): Unit = mode match {
    case (DiagnosticState | OperationsState) => diagPublisher ! mode
    case _                                   ⇒
  }

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = controlCommand match {
    case _: Setup   => validateOneSetup(controlCommand.asInstanceOf[Setup])
    case _: Observe => Accepted(controlCommand.runId)
  }

  override def onSubmit(controlCommand: ControlCommand): Unit =
    commandHandler ! CommandMessageE(Submit(controlCommand, commandResponseAdapter))

  override def onOneway(controlCommand: ControlCommand): Unit = println("One way command received")

  private def getAssemblyConfigs: Future[(TromboneCalculationConfig, TromboneControlConfig)] = {
    val config = ConfigFactory.load("tromboneAssemblyContext.conf")
    Future((TromboneCalculationConfig(config), TromboneControlConfig(config)))
  }

  //#onLocationTrackingEvent-handler
  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {
    trackingEvent match {
      case LocationUpdated(location) =>
        runningHcds = runningHcds + (location.connection → Some(
          location.asInstanceOf[AkkaLocation].component
        ))
      case LocationRemoved(connection) =>
        runningHcds = runningHcds + (connection → None)
    }
    commandHandler ! UpdateHcdLocations(runningHcds)
  }
  //#onLocationTrackingEvent-handler
}
