package csw.apps.containercmd

import java.nio.file.{Files, Path}

import akka.actor.ActorSystem
import akka.typed.ActorRef
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import com.typesafe.config.{Config, ConfigFactory}
import csw.apps.containercmd.cli.{ArgsParser, Options}
import csw.exceptions.{ClusterSeedsNotFound, FileNotFound, LocalFileNotFound, UnableToParseOptions}
import csw.framework.internal.wiring.{Container, FrameworkWiring, Standalone}
import csw.services.BuildInfo
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}
import csw.services.logging.internal.{LogControlMessages, LoggingSystem}
import csw.services.logging.scaladsl.{ComponentLogger, LogAdminActor, LoggingSystemFactory}

import scala.async.Async.{async, await}
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

object ContainerCmd {
  def start(name: String, args: Array[String]): ActorRef[_] =
    new ContainerCmd(name, ClusterAwareSettings).start(args)
}

private[containercmd] class ContainerCmd(
    name: String,
    clusterSettings: ClusterSettings
) extends ComponentLogger.Simple {

  override protected def maybeComponentName() = Some(name)

  lazy val actorSystem: ActorSystem = clusterSettings.system
  lazy val wiring: FrameworkWiring  = FrameworkWiring.make(actorSystem)
  import wiring.actorRuntime._

  def start(args: Array[String]): ActorRef[_] = {
    if (clusterSettings.seedNodes.isEmpty)
      throw ClusterSeedsNotFound
    else
      new ArgsParser().parse(args) match {
        case None ⇒ throw UnableToParseOptions
        case Some(Options(standalone, isLocal, inputFilePath)) =>
          val loggingSystem =
            LoggingSystemFactory.start(BuildInfo.name, BuildInfo.version, clusterSettings.hostname, actorSystem)

          log.debug(s"$name started with following arguments [${args.mkString(",")}]")

          try {
            Await.result(createF(standalone, isLocal, inputFilePath, loggingSystem), 30.seconds)
          } catch {
            case NonFatal(ex) ⇒
              log.error(s"${ex.getMessage}", ex = ex)
              shutdown()
              throw ex
          }
      }
  }

  private def createF(
      standalone: Boolean,
      isLocal: Boolean,
      inputFilePath: Option[Path],
      loggingSystem: LoggingSystem
  ): Future[ActorRef[_]] = {
    async {
      val adminLogActor = actorSystem.spawn(LogAdminActor.behavior(loggingSystem), "log-admin")
      val config        = await(getConfig(isLocal, inputFilePath.get))
      val actorRef      = await(createComponent(standalone, wiring, config, adminLogActor))
      log.info(s"Component is successfully created with actor actorRef $actorRef")
      actorRef
    }
  }

  private def createComponent(
      standalone: Boolean,
      wiring: FrameworkWiring,
      config: Config,
      adminActorRef: ActorRef[LogControlMessages]
  ): Future[ActorRef[_]] = {
    if (standalone) Standalone.spawn(config, wiring, adminActorRef)
    else Container.spawn(config, wiring, adminActorRef)
  }

  private def getConfig(isLocal: Boolean, inputFilePath: Path): Future[Config] = {
    if (isLocal) {
      val config = getConfigFromLocalFile(inputFilePath)
      Future.successful(config)
    } else getConfigFromRemoteFile(inputFilePath)
  }

  private def getConfigFromLocalFile(inputFilePath: Path): Config = {
    if (Files.exists(inputFilePath)) ConfigFactory.parseFile(inputFilePath.toFile)
    else throw LocalFileNotFound(inputFilePath)
  }

  private def getConfigFromRemoteFile(inputFilePath: Path): Future[Config] = async {
    await(wiring.configClientService.getActive(inputFilePath)) match {
      case Some(configData) ⇒ await(configData.toConfigObject)
      case None             ⇒ throw FileNotFound(inputFilePath)
    }
  }

  private def shutdown() = Await.result(wiring.actorRuntime.shutdown(), 10.seconds)
}
