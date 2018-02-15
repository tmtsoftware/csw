package tmt.sequencer.component

import java.nio.file.Paths

import akka.stream.ActorMaterializer
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter.TypedActorSystemOps
import akka.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.ccs.commands.{CommandResponse, ControlCommand}
import csw.messages.framework.ComponentInfo
import csw.messages.location.TrackingEvent
import csw.messages.models.PubSub.PublisherMessage
import csw.messages.params.states.CurrentState
import csw.messages.{CommandResponseManagerMessage, TopLevelActorMessage}
import csw.services.config.api.models.ConfigData
import csw.services.config.api.scaladsl.ConfigClientService
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory
import tmt.sequencer.dsl.{CommandService, ControlDsl}
import tmt.sequencer.engine.EngineBehaviour.EngineAction
import tmt.sequencer.engine.{Engine, EngineBehaviour}
import tmt.services

import scala.async.Async.{async, await}
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContextExecutor, Future}

class SequencerHandlers(
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
    ) {

  implicit val ct: ActorContext[TopLevelActorMessage] = ctx
  implicit val ec: ExecutionContextExecutor           = ctx.executionContext
  implicit val mat: ActorMaterializer                 = ActorMaterializer()(ctx.system.toUntyped)

  override def initialize(): Future[Unit] = async {

    val configClient: ConfigClientService = ConfigClientFactory.clientApi(ctx.system.toUntyped, locationService)

    val maybeData: Option[ConfigData] = await(configClient.getActive(Paths.get(s"/${componentInfo.name}.sc")))

    val outPutFilePath = await {
      maybeData match {
        case Some(configData) => configData.toPath(Paths.get(s"scripts/${componentInfo.name}.sc"))
        case None             => ??? // TODO
      }
    }

    implicit lazy val timeout: Timeout = Timeout(5.seconds)

    val engineActor: ActorRef[EngineAction] = await(ctx.system.systemActorOf(EngineBehaviour.behaviour, "engine"))

    ctx.watch(engineActor)
    val engine1 = SequencerHandlers.dd(ctx.system, engineActor) //TODO: what to do if engine actor dies ? Decide in handlers.
//SequencerHandlers.engine = engine1
    // Load script
    val params: List[String] = List(s"scripts/${componentInfo.name}.sc")
    // Run script using ammonite
    ammonite.Main.main0(params, System.in, System.out, System.err)

//    implicit lazy val timeout: Timeout = Timeout(5.seconds)

//    lazy val engineActor: ActorRef[EngineAction] =
//      Await.result(ctx.system.systemActorOf(EngineBehaviour.behaviour, "engine"), timeout.duration)
//    lazy val engine = new Engine(engineActor, ctx.system)
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = ???

  override def onSubmit(controlCommand: ControlCommand): Unit = ???

  override def onOneway(controlCommand: ControlCommand): Unit = ???

  override def onShutdown(): Future[Unit] = ??? //TODO: clean up engine and other relevant instances

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???
}

object SequencerHandlers extends ControlDsl {
  var engine1: Engine = _
  def dd(system: ActorSystem[Nothing], engineActor: ActorRef[EngineAction]): Engine = {
    val cs: CommandService = new CommandService(new services.LocationService(system))(system.executionContext) //TODO: fix location service
    engine1 = new Engine(engineActor, system)

    engine1
  }

  override def engine: Engine = engine1
}
