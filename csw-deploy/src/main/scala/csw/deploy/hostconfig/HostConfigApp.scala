package csw.deploy.hostconfig

import akka.actor.ActorSystem
import csw.deploy.hostconfig.cli.{ArgsParser, Options}
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

// $COVERAGE-OFF$
class HostConfigApp(clusterSettings: ClusterSettings, startLogging: Boolean = false) extends ComponentLogger.Simple {

  lazy val actorSystem: ActorSystem           = clusterSettings.system
  lazy val wiring: FrameworkWiring            = FrameworkWiring.make(actorSystem)
  private var processes: Set[process.Process] = _

  override protected def componentName() = "HostConfigApp"

  def start(args: Array[String]): Unit =
    new ArgsParser().parse(args).foreach {
      case Options(isLocal, hostConfigPath, Some(containerScript)) =>
        try {
          if (startLogging) wiring.actorRuntime.startLogging()

          val hostConfig = Await.result(
            wiring.configUtils.getConfig(isLocal, hostConfigPath, None),
            10.seconds
          )
          val bootstrapInfo = ConfigParser.parseHost(hostConfig)
          log.info(s"Bootstrapping containers: [${bootstrapInfo.containers}]")
          processes = bootstrapContainers(containerScript, bootstrapInfo)
        } catch {
          case NonFatal(ex) ⇒
            log.error(s"${ex.getMessage}", ex = ex)
            throw ex
        } finally {
          // once all the processes are started for each container,
          // host applications actor system is no longer needed,
          // otherwise it will keep taking part of cluster decisions
          shutdown()
          waitForProcessTermination(processes)
          log.warn("Exiting HostConfigApp as all the processes start by this app are terminated")
        }
    }

  private def bootstrapContainers(containerScript: String, bootstrapInfo: HostBootstrapInfo): Set[process.Process] =
    bootstrapInfo.containers
      .map {
        case ContainerBootstrapInfo(Container, configPath, Remote) ⇒
          executeScript(containerScript, configPath)
        case ContainerBootstrapInfo(Container, configPath, Local) ⇒
          executeScript(containerScript, s"$configPath --local")
        case ContainerBootstrapInfo(Standalone, configPath, Remote) ⇒
          executeScript(containerScript, s"$configPath --standalone")
        case ContainerBootstrapInfo(Standalone, configPath, Local) ⇒
          executeScript(containerScript, s"$configPath --local --standalone")
      }

  private def executeScript(containerScript: String, args: String): process.Process = {
    val command = s"$containerScript $args"
    log.info(s"Executing command : $command")
    command.run()
  }

  private def waitForProcessTermination(processes: Set[process.Process]): Unit = processes.foreach { process ⇒
    val exitCode = process.exitValue()
    log.warn(s"Container exited with code: [$exitCode]")
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
// $COVERAGE-ON$
