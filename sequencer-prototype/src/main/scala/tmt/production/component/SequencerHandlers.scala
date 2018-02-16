package tmt.production.component

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import akka.stream.ActorMaterializer
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter.TypedActorSystemOps
import akka.util.Timeout
import csw.framework.scaladsl.{ComponentHandlers, CurrentStatePublisher}
import csw.messages.ccs.commands.{CommandResponse, ControlCommand}
import csw.messages.framework.ComponentInfo
import csw.messages.location.TrackingEvent
import csw.messages.{CommandResponseManagerMessage, TopLevelActorMessage}
import csw.services.config.api.models.ConfigData
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory
import tmt.shared.Wiring
import tmt.shared.engine.EngineBehaviour
import tmt.shared.engine.EngineBehaviour.EngineAction

import scala.async.Async.{async, await}
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContextExecutor, Future}

class SequencerHandlers(
    ctx: ActorContext[TopLevelActorMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: ActorRef[CommandResponseManagerMessage],
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

  implicit val ct: ActorContext[TopLevelActorMessage] = ctx
  implicit val ec: ExecutionContextExecutor           = ctx.executionContext
  implicit val mat: ActorMaterializer                 = ActorMaterializer()(ctx.system.toUntyped)
  implicit val timeout: Timeout                       = Timeout(5.seconds)

  override def initialize(): Future[Unit] = async {

    val dummyConfigData: ConfigData =
      ConfigData.fromPath(Paths.get("/Users/bharats/projects/csw-prod/sequencer-prototype/scripts/ocs-sequencer.sc"))
//  val configClient: ConfigClientService = ConfigClientFactory.clientApi(ctx.system.toUntyped, locationService)

    val maybeData: Option[ConfigData] = Some(dummyConfigData)
//  val maybeData: Option[ConfigData] = await(configClient.getActive(Paths.get(s"/${componentInfo.name}.sc")))

    val scriptContent: String = await {
      maybeData match {
        case Some(configData) => configData.toStringF
        case None             => ??? // TODO
      }
    }

    val updatedScript = scriptContent.replace("import tmt.development.dsl.Dsl.wiring._", "")

    val path = Files.write(Paths.get(s"scripts/${componentInfo.name}.sc"), updatedScript.getBytes(StandardCharsets.UTF_8)) //TODO: decide on centos charset code

    val engineActor: ActorRef[EngineAction] = await(ctx.system.systemActorOf(EngineBehaviour.behaviour, "engine"))

    val wiring = new Wiring(ctx.system, engineActor)

    ctx.watch(engineActor) //TODO: what to do if engine actor dies ? Decide in handlers.

//    Dsl.make(ctx.system, engineActor)

    // Load script
    val params: List[String] = List(path.toString)

    ammonite
      .Main(predefCode = "import wiring._")
      .run("wiring" -> wiring) //TODO: decide on main and main0
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {}

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = {
    CommandResponse.Accepted(controlCommand.runId)
  }

  override def onSubmit(controlCommand: ControlCommand): Unit = {}

  override def onOneway(controlCommand: ControlCommand): Unit = {}

  override def onShutdown(): Future[Unit] = { Future.successful(()) } //TODO: clean up engine and other relevant instances

  override def onGoOffline(): Unit = {}

  override def onGoOnline(): Unit = {}
}
