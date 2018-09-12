package tmt.production.component

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.stream.ActorMaterializer
import akka.util.Timeout
import csw.framework.CurrentStatePublisher
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.TopLevelActorMessage
import csw.messages.commands.{CommandResponse, ControlCommand}
import csw.messages.framework.ComponentInfo
import csw.services.location.api.models.TrackingEvent
import csw.services.command.CommandResponseManager
import csw.services.config.api.models.ConfigData
import tmt.production.dsl.Dsl
import tmt.shared.Wiring
import tmt.shared.engine.EngineBehavior
import tmt.shared.engine.EngineBehavior.EngineAction

import scala.async.Async.{async, await}
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContextExecutor, Future}

class SequencerHandlers(
    ctx: ActorContext[TopLevelActorMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: CommandResponseManager,
    currentStatePublisher: CurrentStatePublisher,
    cswCtx: CswContext
) extends ComponentHandlers(
      ctx,
      componentInfo,
      cswCtx
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

    val updatedScript = scriptContent.replace(
      "import tmt.development.dsl.Dsl.wiring._",
      "val wiring = tmt.production.dsl.Dsl.wiring; import wiring._"
    )

    val path = Files.write(Paths.get(s"scripts/${componentInfo.name}.sc"), updatedScript.getBytes(StandardCharsets.UTF_8)) //TODO: decide on centos charset code

    val engineActor: ActorRef[EngineAction] = await(ctx.system.systemActorOf(EngineBehavior.behavior, "engine"))
    Dsl.wiring = new Wiring(ctx.system, engineActor, cswCtx.locationService)

    ctx.watch(engineActor) //TODO: what to do if engine actor dies ? Decide in handlers. Decide if we need engineActor to be persistent actor

    val params: List[String] = List(path.toString)
    ammonite.Main
      .main0(params, System.in, System.out, System.err) //TODO: decide which threadpool to provide for this method's execution
  }

  def onCommandsReceived(controlCommands: Seq[ControlCommand]): Unit = {
    Dsl.wiring.engine.pushAll(controlCommands) //TODO: fix interruption of commands
  }

  override def onShutdown(): Future[Unit] = async {
    //Delete the script from local disk
    //kill the ammonite instance
  } //TODO: clean up engine and other relevant instances and ammonite instance

  override def onSubmit(controlCommand: ControlCommand): Unit              = {}
  override def onOneway(controlCommand: ControlCommand): Unit              = {}
  override def onGoOffline(): Unit                                         = {}
  override def onGoOnline(): Unit                                          = {}
  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {}
  override def validateCommand(controlCommand: ControlCommand): CommandResponse = {
    CommandResponse.Accepted(controlCommand.runId)
  }
}
