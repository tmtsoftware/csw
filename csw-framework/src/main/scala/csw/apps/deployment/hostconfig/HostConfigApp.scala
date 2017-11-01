package csw.apps.deployment.hostconfig

import akka.actor.ActorSystem
import csw.apps.deployment.hostconfig.cli.{ArgsParser, Options}
import csw.framework.internal.configparser.ConfigParser
import csw.framework.internal.wiring.FrameworkWiring
import csw.framework.models.ConfigFileLocation.{Local, Remote}
import csw.framework.models.ContainerMode.{Container, Standalone}
import csw.framework.models.{ContainerBootstrapInfo, HostBootstrapInfo}
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}
import csw.services.logging.scaladsl.ComponentLogger

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble
import scala.sys.process
import scala.sys.process.stringToProcess
import scala.util.control.NonFatal

class HostConfigApp(clusterSettings: ClusterSettings, startLogging: Boolean = false) extends ComponentLogger.Simple {

  lazy val actorSystem: ActorSystem = clusterSettings.system
  lazy val wiring: FrameworkWiring  = FrameworkWiring.make(actorSystem)

  override protected def componentName() = "HostConfigApp"

  def start(args: Array[String]): Unit =
    new ArgsParser().parse(args).foreach {
      case Options(isLocal, hostConfigPath) =>
        try {
          if (startLogging) wiring.actorRuntime.startLogging()

          val hostConfig = Await.result(
            wiring.configUtils.getConfig(isLocal, hostConfigPath, None),
            10.seconds
          )
          val bootstrapInfo = ConfigParser.parseHost(hostConfig)
          log.info(s"Bootstrapping containers: [${bootstrapInfo.containers}]")
          val processes = bootstrapContainers(bootstrapInfo)

          processes.foreach {
            case (container, process) ⇒
              log.info(s"Status of container process for $container => isAlive = ${process.isAlive()}")
          }

        } catch {
          case NonFatal(ex) ⇒
            log.error(s"${ex.getMessage}", ex = ex)
            shutdown()
            throw ex
        }
    }

  private def bootstrapContainers(bootstrapInfo: HostBootstrapInfo): Set[(String, process.Process)] = {
    bootstrapInfo.containers
      .map {
        case ContainerBootstrapInfo(executable, Container, configPath, Remote) ⇒
          executable -> s"$executable $configPath".run()
        case ContainerBootstrapInfo(executable, Container, configPath, Local) ⇒
          executable → s"$executable $configPath --local".run()
        case ContainerBootstrapInfo(executable, Standalone, configPath, Remote) ⇒
          executable → s"$executable $configPath --standalone".run()
        case ContainerBootstrapInfo(executable, Standalone, configPath, Local) ⇒
          executable → s"$executable $configPath --local --standalone".run()
      }
  }

  private def shutdown() = Await.result(wiring.actorRuntime.shutdown(), 10.seconds)
}

object HostConfigApp extends App {
  if (ClusterAwareSettings.seedNodes.isEmpty) {
    println(
      "clusterSeeds setting is not specified either as env variable or system property. Please check online documentation for this set-up."
    )
  } else {
    new HostConfigApp(ClusterAwareSettings, startLogging = true).start(args)
  }
}
