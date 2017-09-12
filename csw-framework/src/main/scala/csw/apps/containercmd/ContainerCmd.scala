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
import csw.services.logging.scaladsl.LoggingSystemFactory

import scala.async.Async.{async, await}
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future}

class ContainerCmd(clusterSettings: ClusterSettings = ClusterAwareSettings, startLogging: Boolean = false) {

  lazy val actorSystem: ActorSystem = clusterSettings.system
  lazy val wiring: FrameworkWiring  = FrameworkWiring.make(actorSystem)

  def start(args: Array[String]): Option[ActorRef[_]] = {
    if (clusterSettings.seedNodes.isEmpty) {
      println(
        "clusterSeeds setting is not specified either as env variable or system property. Please check online documentation for this set-up."
      )
      None
    } else {
      new ArgsParser().parse(args).map {
        case Options(standalone, isLocal, inputFilePath) =>
          if (startLogging) {
            LoggingSystemFactory.start(BuildInfo.name, BuildInfo.version, clusterSettings.hostname, actorSystem)
          }

          try {
            createComponent(standalone, wiring, getConfig(isLocal, inputFilePath.get))
          } catch {
            case ex: Exception ⇒
              println(s"log.error(${ex.getMessage}, $ex)")
              shutdown
              throw ex
          }
      }
    }
  }

  def shutdown: Future[Done] = {
    wiring.actorRuntime.shutdown()
  }

  private def createComponent(standalone: Boolean, wiring: FrameworkWiring, config: Config): ActorRef[_] = {
    if (standalone) Standalone.spawn(config, wiring)
    else Container.spawn(config, wiring)
  }

  private def getConfig(isLocal: Boolean, inputFilePath: Path): Config = {
    if (isLocal) {
      if (Files.exists(inputFilePath)) ConfigFactory.parseFile(inputFilePath.toFile)
      else throw LocalFileNotFound(inputFilePath)
    } else {
      import wiring.actorRuntime.{ec, mat}
      val configF = async {
        val maybeData = await(wiring.configService.getActive(inputFilePath))

        if (maybeData.isDefined) await(maybeData.get.toConfigObject)
        else throw FileNotFound(inputFilePath)
      }
      Await.result(configF, 10.seconds)
    }
  }
}
