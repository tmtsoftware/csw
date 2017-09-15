package csw.apps.containercmd

import java.nio.file.{Files, Path}

import akka.Done
import akka.actor.ActorSystem
import akka.typed.ActorRef
import com.typesafe.config.{Config, ConfigFactory}
import csw.apps.containercmd.cli.{ArgsParser, Options}
import csw.apps.containercmd.exceptions.Exceptions.{FileNotFound, LocalFileNotFound}
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

  override protected val componentName: String = name
  lazy val actorSystem: ActorSystem            = clusterSettings.system
  lazy val wiring: FrameworkWiring             = FrameworkWiring.make(actorSystem)
  import wiring.actorRuntime.{ec, mat}

  def start(args: Array[String]): Future[Option[ActorRef[_]]] = {
    def createF(standalone: Boolean, isLocal: Boolean, inputFilePath: Option[Path]): Future[ActorRef[_]] = {
      async {
        val configF = await(getConfig(isLocal, inputFilePath.get))
        await(createComponent(standalone, wiring, configF))
      } recover {
        case NonFatal(ex) ⇒
          log.error(s"${ex.getMessage}", ex = ex)
          shutdown()
          throw ex
      }
    }

    if (clusterSettings.seedNodes.isEmpty) {
      println(
        "clusterSeeds setting is not specified either as env variable or system property. Please check online documentation for this set-up."
      )
      Future.successful(None)
    } else {
      val maybeOptions = new ArgsParser().parse(args)
      maybeOptions match {
        case Some(Options(standalone, isLocal, inputFilePath)) =>
          if (startLogging)
            LoggingSystemFactory.start(BuildInfo.name, BuildInfo.version, clusterSettings.hostname, actorSystem)

          log.debug(s"$componentName started with following arguments [${args.mkString(",")}]")

          createF(standalone, isLocal, inputFilePath).map { ref ⇒
            log.info(s"Component is successfully created with actor ref $ref")
            Some(ref)
          }

        case _ ⇒ Future.successful(None)
      }
    }
  }

  def shutdown(): Future[Done] = wiring.actorRuntime.shutdown()

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
