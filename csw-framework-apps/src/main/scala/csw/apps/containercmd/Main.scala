package csw.apps.containercmd

import java.nio.file.{Files, Path}

import akka.Done
import akka.actor.ActorSystem
import akka.typed.ActorRef
import com.typesafe.config.{Config, ConfigFactory}
import csw.apps.containercmd.cli.{ArgsParser, Options}
import csw.common.framework.internal.wiring.{Container, FrameworkWiring, Standalone}
import csw.common.framework.models.{ContainerMessage, SupervisorExternalMessage}
import csw.services.BuildInfo
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}
import csw.services.logging.scaladsl.LoggingSystemFactory

import scala.concurrent.Future
import scala.util.{Failure, Success}

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

      var config: Config = ConfigFactory.empty()

      //FIXME to use log statements in below code block
      wiring.configService.exists(inputFilePath).onComplete {
        case Success(fileExists) ⇒
          if (fileExists) {
            wiring.configService.getActive(inputFilePath).onComplete {
              case Success(Some(configData)) ⇒
                configData.toConfigObject.onComplete {
                  case Success(configObject) ⇒ config = configObject
                  case Failure(exception)    ⇒ println(s"$exception")
                }
              case Success(None)      ⇒ println(s"Could not load data for file at path $inputFilePath")
              case Failure(exception) ⇒ println(s"$exception")
            }
          } else println(s"File at path $inputFilePath does not exist in config service")
        case Failure(exception) ⇒ println(s"$exception")
      }

      if (!config.isEmpty) config
      else throw new RuntimeException("Could not create config object from Configuration service")
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
