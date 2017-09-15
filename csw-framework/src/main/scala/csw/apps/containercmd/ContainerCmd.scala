package csw.apps.containercmd

import java.nio.file.{Files, Path}

import akka.Done
import akka.actor.ActorSystem
import akka.typed.ActorRef
import com.typesafe.config.{Config, ConfigFactory}
import csw.apps.containercmd.cli.{ArgsParser, Options}
import csw.apps.containercmd.exceptions.Exceptions.{FileNotFound, LocalFileNotFound}
import csw.common.framework.exceptions.ClusterSeedsNotFound
import csw.common.framework.internal.wiring.{Container, FrameworkWiring, Standalone}
import csw.services.BuildInfo
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}
import csw.services.logging.scaladsl.{ComponentLogger, LoggingSystemFactory}

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.util.control.NonFatal

object ContainerCmd {
  def start(name: String, args: Array[String]): Future[Option[ActorRef[_]]] =
    new ContainerCmd(name, clusterSettings = ClusterAwareSettings).start(args)
}

private[containercmd] class ContainerCmd(
    name: String,
    clusterSettings: ClusterSettings,
    startLogging: Boolean = true
) extends ComponentLogger.Simple {

  lazy val actorSystem: ActorSystem = clusterSettings.system
  lazy val wiring: FrameworkWiring  = FrameworkWiring.make(actorSystem)
  import wiring.actorRuntime.{ec, mat}

  override protected val componentName: String = name

  def start(args: Array[String]): Future[Option[ActorRef[_]]] = async {
    if (startLogging)
      LoggingSystemFactory.start(BuildInfo.name, BuildInfo.version, clusterSettings.hostname, actorSystem)

    log.debug(s"$componentName started with following arguments [${args.mkString(",")}]")

    if (clusterSettings.seedNodes.isEmpty) {
      val exception = ClusterSeedsNotFound()
      log.error(exception.getMessage, ex = exception)
      throw exception
    } else {
      val maybeOptions = new ArgsParser().parse(args)
      maybeOptions match {
        case Some(Options(standalone, isLocal, inputFilePath)) =>
          val created = await(createF(standalone, isLocal, inputFilePath))
          log.info(s"Component is successfully created with actor ref $created")
          Some(created)
        case _ ⇒ None
      }
    }
  }

  def shutdown(): Future[Done] = wiring.actorRuntime.shutdown()

  private def createF(
      standalone: Boolean,
      isLocal: Boolean,
      inputFilePath: Option[Path]
  ): Future[ActorRef[_]] =
    async {
      val configF = await(getConfig(isLocal, inputFilePath.get))
      await(createComponent(standalone, wiring, configF))
    } recover {
      case NonFatal(ex) ⇒
        log.error(s"${ex.getMessage}", ex = ex)
        shutdown()
        throw ex
    }

  private def createComponent(standalone: Boolean, wiring: FrameworkWiring, config: Config): Future[ActorRef[_]] = {
    if (standalone) Standalone.spawn(config, wiring)
    else Container.spawn(config, wiring)
  }

  private def getConfig(isLocal: Boolean, inputFilePath: Path): Future[Config] = async {
    if (isLocal) {
      if (Files.exists(inputFilePath)) ConfigFactory.parseFile(inputFilePath.toFile)
      else throw LocalFileNotFound(inputFilePath)
    } else {
      val maybeData = await(wiring.configService.getActive(inputFilePath))

      if (maybeData.isDefined) await(maybeData.get.toConfigObject)
      else throw FileNotFound(inputFilePath)
    }
  }
}
