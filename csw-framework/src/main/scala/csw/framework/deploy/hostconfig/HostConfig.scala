package csw.framework.deploy.hostconfig

import akka.actor.ActorSystem
import akka.actor.CoordinatedShutdown.Reason
import csw.framework.commons.CoordinatedShutdownReasons.ApplicationFinishedReason
import csw.framework.deploy.hostconfig.cli.{ArgsParser, Options}
import csw.framework.exceptions.{ClusterSeedsNotFound, UnableToParseOptions}
import csw.framework.internal.configparser.ConfigParser
import csw.framework.internal.wiring.FrameworkWiring
import csw.framework.models.ConfigFileLocation.{Local, Remote}
import csw.framework.models.ContainerMode.{Container, Standalone}
import csw.framework.models.{ContainerBootstrapInfo, HostBootstrapInfo}
import csw.location.api.commons.{ClusterAwareSettings, ClusterSettings}
import csw.logging.scaladsl.{Logger, LoggerFactory}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble
import scala.sys.process
import scala.sys.process.stringToProcess
import scala.util.control.NonFatal

// $COVERAGE-OFF$
object HostConfig {

  /**
   * Utility for starting multiple Containers on a single host machine
   * @param name              The name to be used for the main app which uses this utility
   * @param args              The command line args accepted in the main app which uses this utility
   */
  def start(name: String, args: Array[String]): Unit =
    new HostConfig(name, ClusterAwareSettings, startLogging = true).start(args)
}

private[hostconfig] class HostConfig(name: String, clusterSettings: ClusterSettings, startLogging: Boolean = false) {
  private val log: Logger = new LoggerFactory(name).getLogger

  private lazy val actorSystem: ActorSystem   = clusterSettings.system
  private lazy val wiring: FrameworkWiring    = FrameworkWiring.make(actorSystem)
  private var processes: Set[process.Process] = _

  def start(args: Array[String]): Unit =
    if (clusterSettings.seedNodes.isEmpty)
      throw ClusterSeedsNotFound
    else
      new ArgsParser(name).parse(args) match {
        case Some(Options(isLocal, hostConfigPath, Some(containerScript))) =>
          try {
            if (startLogging) wiring.actorRuntime.startLogging(name)

            val hostConfig    = Await.result(wiring.configUtils.getConfig(isLocal, hostConfigPath, None), 10.seconds)
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
            shutdown(ApplicationFinishedReason)
            waitForProcessTermination(processes)
            log.warn("Exiting HostConfigApp as all the processes start by this app are terminated")
          }
        case _ ⇒ throw UnableToParseOptions
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

  // spawn a child process and start a container
  private def executeScript(containerScript: String, args: String): process.Process = {
    val command = s"$containerScript $args"
    log.info(s"Executing command : $command")
    command.run()
  }

  private def waitForProcessTermination(processes: Set[process.Process]): Unit = processes.foreach { process ⇒
    val exitCode = process.exitValue()
    log.warn(s"Container exited with code: [$exitCode]")
  }

  private def shutdown(reason: Reason) = Await.result(wiring.actorRuntime.shutdown(reason), 10.seconds)
}
// $COVERAGE-ON$
