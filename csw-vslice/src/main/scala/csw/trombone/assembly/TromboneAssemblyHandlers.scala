package csw.trombone.assembly

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter._
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import com.typesafe.config.ConfigFactory
import csw.exceptions.{FailureRestart, FailureStop}
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.CommandMessage.{Oneway, Submit}
import csw.messages._
import csw.messages.ccs.CommandIssue.UnsupportedCommandIssue
import csw.messages.ccs.commands.CommandResponse.{Accepted, Invalid}
import csw.messages.ccs.commands._
import csw.messages.framework.ComponentInfo
import csw.messages.location._
import csw.messages.models.PubSub.PublisherMessage
import csw.messages.params.states.CurrentState
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory
import csw.trombone.assembly.AssemblyCommandHandlerMsgs.CommandMessageE
import csw.trombone.assembly.CommonMsgs.UpdateHcdLocations
import csw.trombone.assembly.ParamValidation._
import csw.trombone.assembly.actors.{DiagPublisher, TromboneAssemblyCommandBehaviorFactory, TrombonePublisher}

import scala.async.Async.{async, await}
import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, Future}

//#component-factory
class TromboneAssemblyBehaviorFactory extends ComponentBehaviorFactory {
  override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      commandResponseManager: ActorRef[CommandResponseManagerMessage],
      pubSubRef: ActorRef[PublisherMessage[CurrentState]],
      locationService: LocationService,
      loggerFactory: LoggerFactory
  ): ComponentHandlers =
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
) extends ComponentHandlers(
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

  implicit val typed                                            = ctx.system
  implicit val settings                                         = TestKitSettings(typed)
  private val commandResponseAdapter: ActorRef[CommandResponse] = TestProbe[CommandResponse].ref

  private val configClient = ConfigClientFactory.clientApi(ctx.system.toUntyped, locationService)

  case class HcdNotFoundException()        extends FailureRestart("Could not resolve hcd location. Initialization failure.")
  case class ConfigNotAvailableException() extends FailureStop("Configuration not available. Initialization failure.")

  implicit var ac: AssemblyContext     = _
  implicit val ec: ExecutionContext    = ctx.executionContext
  implicit val atorSystem: ActorSystem = ctx.system.toUntyped
  implicit val mat: Materializer       = ActorMaterializer()

  def onRun(): Future[Unit] = Future.unit

  //#initialize-handler
  def initialize(): Future[Unit] = async {
    // fetch config (preferably from configuration service)
    val (calculationConfig, controlConfig) = await(getAssemblyConfigsLocal)
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
        // #failureRestart-Exception
        locationService.resolve(hcd.of[AkkaLocation], 5.seconds).map {
          case Some(akkaLocation) ⇒
            runningHcds = runningHcds.updated(hcdConnection.get, Some(new ComponentRef(akkaLocation)))
            diagPublisher = ctx.spawnAnonymous(DiagPublisher.make(ac, runningHcds(maybeConnection.get), Some(eventPublisher)))
            commandHandler =
              ctx.spawnAnonymous(new TromboneAssemblyCommandBehaviorFactory().make(ac, runningHcds, Some(eventPublisher)))
          case None ⇒
            // Hcd connection could not be resolved for this Assembly. One option to handle this could be to automatic restart which can give enough time
            // for the Hcd to be available
            throw HcdNotFoundException()
          // #failureRestart-Exception
        }
      case None ⇒ Future.successful(Unit)
    }
  }
  //#initialize-handler

  override def onShutdown(): Future[Unit] = {
    Future.successful(println("Received Shutdown"))
  }

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

  // #validateCommand-handler
  override def validateCommand(controlCommand: ControlCommand): CommandResponse = controlCommand match {
    case _: Setup   ⇒ validateOneSetup(controlCommand.asInstanceOf[Setup])
    case _: Observe ⇒ Accepted(controlCommand.runId)
    case x          ⇒ Invalid(controlCommand.runId, UnsupportedCommandIssue(s"command $x is not supported by this component."))
  }
  // #validateCommand-handler

  // #onSubmit-handler
  override def onSubmit(controlCommand: ControlCommand): Unit = {
    // commandHandler is a worker actor created for this component to process commands
    commandHandler ! CommandMessageE(Submit(controlCommand, commandResponseAdapter))
  }
  //#onSubmit-handler

  // #onOneway-handler
  override def onOneway(controlCommand: ControlCommand): Unit = {
    // commandHandler is a worker actor created for this component to process commands
    commandHandler ! CommandMessageE(Oneway(controlCommand, commandResponseAdapter))
  }
  //#onOneway-handler

  private def getAssemblyConfigsLocal: Future[(TromboneCalculationConfig, TromboneControlConfig)] = {
    val config = ConfigFactory.load("tromboneAssemblyContext.conf")
    Future((TromboneCalculationConfig(config), TromboneControlConfig(config)))
  }

  // #failureStop-Exception
  private def getAssemblyConfigs: Future[(TromboneCalculationConfig, TromboneControlConfig)] = {
    configClient.getActive(Paths.get("tromboneAssemblyContext.conf")).flatMap {
      case Some(config) ⇒ config.toConfigObject.map(x ⇒ (TromboneCalculationConfig(x), TromboneControlConfig(x)))
      case None         ⇒
        // required configuration could not be found in the configuration service. Component can choose to stop until the configuration is made available in the
        // cofiguration service and started again
        throw ConfigNotAvailableException()
    }
  }
  // #failureStop-Exception

  //#onLocationTrackingEvent-handler
  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {
    trackingEvent match {
      case LocationUpdated(location) =>
        runningHcds = runningHcds + (location.connection → Some(
          new ComponentRef(location.asInstanceOf[AkkaLocation])
        ))
      case LocationRemoved(connection) =>
        runningHcds = runningHcds + (connection → None)
    }
    commandHandler ! UpdateHcdLocations(runningHcds)
  }
  //#onLocationTrackingEvent-handler
}
