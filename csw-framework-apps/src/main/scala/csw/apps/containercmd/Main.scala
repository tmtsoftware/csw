package csw.apps.containercmd

import java.nio.file.{Files, Path}

import akka.Done
import akka.actor.ActorSystem
import akka.typed.ActorRef
import com.typesafe.config.{Config, ConfigFactory}
import csw.apps.containercmd.cli.{ArgsParser, Options}
import csw.apps.containercmd.exceptions.Exceptions.{FileDataNotFound, FileNotFound}
import csw.common.framework.internal.wiring.{Container, FrameworkWiring, Standalone}
import csw.common.framework.models.{ContainerMessage, SupervisorExternalMessage}
import csw.services.BuildInfo
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}
import csw.services.logging.scaladsl.LoggingSystemFactory

import scala.async.Async.{async, await}
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future}

class Main(clusterSettings: ClusterSettings, startLogging: Boolean = false) {

  lazy val actorSystem: ActorSystem = clusterSettings.system
  lazy val wiring: FrameworkWiring  = FrameworkWiring.make(actorSystem)

  def start(args: Array[String]): Option[ActorRef[SupervisorExternalMessage with ContainerMessage]] = {
    new ArgsParser().parse(args).map {
      case Options(standalone, isLocal, inputFilePath) =>
        if (startLogging) {
          LoggingSystemFactory.start(BuildInfo.name, BuildInfo.version, clusterSettings.hostname, actorSystem)
        }

        try {
          createComponent(standalone, wiring, getConfig(isLocal, inputFilePath.get))
        } catch {
          case ex: Exception ⇒ {
            shutdown
            println(s"log.error(${ex.getMessage}, ex)")
            throw ex
          }
        }
    }
  }

  def shutdown: Future[Done] = {
    wiring.actorRuntime.shutdown()
  }

  private def createComponent(standalone: Boolean,
                              wiring: FrameworkWiring,
                              config: Config): ActorRef[SupervisorExternalMessage with ContainerMessage] = {
    if (standalone) Standalone.spawn(config, wiring)
    else Container.spawn(config, wiring)
  }

  private def getConfig(isLocal: Boolean, inputFilePath: Path): Config = {
    if (isLocal) {
      if (Files.exists(inputFilePath)) ConfigFactory.parseFile(inputFilePath.toFile)
      else throw new RuntimeException("Config file does not exist")
    } else {
      import wiring.actorRuntime.{ec, mat}

      val configF = async {
        val fileExists = await(wiring.configService.exists(inputFilePath))

        val maybeData =
          if (fileExists) await(wiring.configService.getActive(inputFilePath))
          else throw FileNotFound(inputFilePath)

        if (maybeData.isDefined) await(maybeData.get.toConfigObject)
        else throw FileDataNotFound(inputFilePath)
      }

      Await.result(configF, 10.seconds)
    }
  }
}

object Main extends App {
  if (ClusterAwareSettings.seedNodes.isEmpty) {
    println(
      "clusterSeeds setting is not specified either as env variable or system property. Please check online documentation for this set-up."
    )
  } else {
    new Main(ClusterAwareSettings, startLogging = true).start(args)
  }

}
