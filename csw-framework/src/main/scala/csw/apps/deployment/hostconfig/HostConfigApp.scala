package csw.apps.deployment.hostconfig

import akka.actor.ActorSystem
import csw.apps.deployment.hostconfig.cli.{ArgsParser, Options}
import csw.exceptions.UnableToParseOptions
import csw.framework.internal.configparser.ConfigParser
import csw.framework.internal.wiring.FrameworkWiring
import csw.framework.models.ConfigFileLocation.{Local, Remote}
import csw.framework.models.{ContainerBootstrapInfo, HostBootstrapInfo}
import csw.framework.models.ContainerMode.{Container, Standalone}
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}
import csw.services.logging.scaladsl.ComponentLogger

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble
import scala.sys.process.stringToProcess
import scala.util.control.NonFatal

class HostConfigApp(clusterSettings: ClusterSettings, startLogging: Boolean = false) extends ComponentLogger.Simple {

  lazy val actorSystem: ActorSystem = clusterSettings.system
  lazy val wiring: FrameworkWiring  = FrameworkWiring.make(actorSystem)

  override protected def componentName() = "HostConfigApp"

  def start(args: Array[String]): Unit =
    new ArgsParser().parse(args) match {
      case None ⇒ throw UnableToParseOptions
      case Some(Options(isLocal, hostConfigPath)) =>
        try {
          if (startLogging) wiring.actorRuntime.startLogging()

          val hostConfig = Await.result(
            wiring.configUtils.getConfig(isLocal, hostConfigPath, None, wiring.configClientService),
            10.seconds
          )
          val bootstrapInfo = ConfigParser.parseHost(hostConfig)
          log.info(s"Bootstrapping containers: [${bootstrapInfo.containers}]")
          bootstrapContainers(bootstrapInfo)

        } catch {
          case NonFatal(ex) ⇒
            log.error(s"${ex.getMessage}", ex = ex)
            shutdown()
            throw ex
        }
    }

  private def bootstrapContainers(bootstrapInfo: HostBootstrapInfo): Unit = {
    bootstrapInfo.containers
      .foreach {
        case container @ ContainerBootstrapInfo(_, Container, _, Remote) ⇒
          s"${container.containerCmdApp} ${container.configFilePath}".run()
        case container @ ContainerBootstrapInfo(_, Container, _, Local) ⇒
          s"${container.containerCmdApp} ${container.configFilePath} --local".run()
        case container @ ContainerBootstrapInfo(_, Standalone, _, Remote) ⇒
          s"${container.containerCmdApp} ${container.configFilePath} --standalone".run()
        case container @ ContainerBootstrapInfo(_, Standalone, _, Local) ⇒
          s"${container.containerCmdApp} ${container.configFilePath} --local --standalone".run()
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
    val args = Array("--local", "/Users/pritamkadam/Downloads/host-config.conf")
    new HostConfigApp(ClusterAwareSettings.joinLocal(3552), startLogging = true).start(args)
  }
}
