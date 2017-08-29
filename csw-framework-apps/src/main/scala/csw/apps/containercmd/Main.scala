package csw.apps.containercmd

import java.nio.file.{Files, Path, Paths}

import akka.typed.ActorRef
import com.typesafe.config.{Config, ConfigFactory}
import csw.apps.containercmd.cli.{ArgsParser, Options}
import csw.common.framework.internal.wiring.{Container, FrameworkWiring, Standalone}
import csw.common.framework.models.{ContainerMessage, SupervisorExternalMessage}
import csw.services.BuildInfo
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}
import csw.services.logging.scaladsl.LoggingSystemFactory

class Main(clusterSettings: ClusterSettings, startLogging: Boolean = false) {

  def start(args: Array[String]): Option[Any] = {
    new ArgsParser().parse(args).map {
      case Options(standalone, isLocal, inputFilePath) =>
        val actorSystem = clusterSettings.system
        if (startLogging) {
          LoggingSystemFactory.start(
            BuildInfo.name,
            BuildInfo.version,
            clusterSettings.hostname,
            actorSystem
          )
        }

        val wiring = FrameworkWiring.make(actorSystem)
        try {
          createComponent(standalone, wiring, getConfig(isLocal, inputFilePath.get))
        } catch {
          case ex: Exception ⇒ {
            wiring.actorRuntime.shutdown()
            println(s"log.error(${ex.getMessage}, ex)")
          }
        }
    }
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
      // Fixme : change when integrated with ConfigService
      ConfigFactory.parseFile(inputFilePath.toFile)
      //fetch from Config service
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
