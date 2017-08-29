package csw.apps.containercmd

import java.nio.file.Path

import com.typesafe.config.{Config, ConfigFactory}
import csw.apps.containercmd.cli.{ArgsParser, Options}
import csw.common.framework.internal.wiring.{Container, FrameworkWiring, Standalone}
import csw.services.BuildInfo
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}
import csw.services.logging.scaladsl.LoggingSystemFactory

class Main(clusterSettings: ClusterSettings, startLogging: Boolean = false) {

  def start(args: Array[String]): Any = {
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
            println("log.error(ex.getMessage, ex = ex)")
          }
        }
    }
  }

  private def createComponent(standalone: Boolean, wiring: FrameworkWiring, config: Config) = {
    if (standalone) Standalone.spawn(config, wiring)
    else Container.spawn(config, wiring)
  }

  private def getConfig(isLocal: Boolean, inputFilePath: Path): Config = {
    if (isLocal) {
      ConfigFactory.parseFile(inputFilePath.toFile)
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
