package csw.framework.deploy.hostconfig

import java.nio.file.Path

import akka.actor.CoordinatedShutdown.Reason
import csw.framework.commons.CoordinatedShutdownReasons.ApplicationFinishedReason
import csw.framework.deploy.hostconfig.cli.{ArgsParser, Options}
import csw.framework.exceptions.UnableToParseOptions
import csw.framework.internal.configparser.ConfigParser
import csw.framework.internal.wiring.FrameworkWiring
import csw.framework.models.ConfigFileLocation.{Local, Remote}
import csw.framework.models.ContainerMode.{Container, Standalone}
import csw.framework.models.{ContainerBootstrapInfo, HostBootstrapInfo}
import csw.location.client.utils.LocationServerStatus
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.util.control.NonFatal

// $COVERAGE-OFF$
object HostConfig {

  /**
   * Utility for starting multiple Containers on a single host machine
   *
   * @param name              The name to be used for the main app which uses this utility
   * @param args              The command line args accepted in the main app which uses this utility
   */
  def start(name: String, args: Array[String]): Unit =
    new HostConfig(name, startLogging = true).start(args)
}

private[hostconfig] class HostConfig(name: String, startLogging: Boolean = false) {
  private val log: Logger             = new LoggerFactory(name).getLogger
  private val timeout: FiniteDuration = 10.seconds

  private lazy val wiring: FrameworkWiring = new FrameworkWiring

  def start(args: Array[String]): Unit =
    new ArgsParser(name).parse(args) match {
      case Some(Options(isLocal, hostConfigPath, Some(containerScript))) =>
        LocationServerStatus.requireUpLocally()
        run(isLocal, hostConfigPath, containerScript)

      case _ ⇒ throw UnableToParseOptions
    }

  def run(isLocal: Boolean, hostConfigPath: Option[Path], containerScript: String): Unit =
    try {
      if (startLogging) wiring.actorRuntime.startLogging(name)

      val hostConfig    = Await.result(wiring.configUtils.getConfig(isLocal, hostConfigPath, None), timeout)
      val bootstrapInfo = ConfigParser.parseHost(hostConfig)
      log.info(s"Bootstrapping containers: [${bootstrapInfo.containers}]")
      val processes = bootstrapContainers(containerScript, bootstrapInfo)
      val pids      = processes.map(_.pid())
      log.info(s"Started processes with following PID's: ${pids.mkString("[", ", ", "]")}")
    } catch {
      case NonFatal(ex) ⇒
        log.error(s"${ex.getMessage}", ex = ex)
        throw ex
    } finally {
      log.info("Exiting host config application.")
      // once all the processes are started for each container,
      // host applications actor system is no longer needed
      shutdown(ApplicationFinishedReason)
    }

  private def bootstrapContainers(containerScript: String, bootstrapInfo: HostBootstrapInfo): Set[Process] =
    bootstrapInfo.containers
      .map {
        case ContainerBootstrapInfo(Container, configPath, Remote) ⇒
          executeScript(containerScript, configPath)
        case ContainerBootstrapInfo(Container, configPath, Local) ⇒
          executeScript(containerScript, s"$configPath", "--local")
        case ContainerBootstrapInfo(Standalone, configPath, Remote) ⇒
          executeScript(containerScript, s"$configPath", "--standalone")
        case ContainerBootstrapInfo(Standalone, configPath, Local) ⇒
          executeScript(containerScript, s"$configPath", "--local", "--standalone")
      }

  // spawn a child process and start a container
  def executeScript(containerScript: String, args: String*): Process = {
    val cmd = containerScript +: args
    log.info(s"Executing command : ${cmd.mkString(" ")}")
    new ProcessBuilder(cmd: _*).start()
  }

  private def shutdown(reason: Reason) = Await.result(wiring.actorRuntime.shutdown(reason), timeout)
}
// $COVERAGE-ON$
