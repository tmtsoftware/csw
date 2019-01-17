package csw.config.server

import csw.aas.core.deployment.AASResolutionFailed
import csw.config.server.cli.{ArgsParser, Options}
import csw.config.server.commons.ConfigServerLogger
import csw.config.server.commons.CoordinatedShutdownReasons.FailureReason
import csw.config.server.http.HttpService
import csw.location.client.utils.LocationServerStatus
import csw.logging.api.scaladsl.Logger
import csw.services.BuildInfo
import org.tmatesoft.svn.core.SVNException

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

/**
 * Application object to start the ConfigServer from command line.
 */
object Main {
  private val name        = BuildInfo.name
  private val log: Logger = ConfigServerLogger.getLogger

  def main(args: Array[String]): Unit = start(args, startLogging = true)

  def start(args: Array[String], startLogging: Boolean = false): Option[HttpService] =
    new ArgsParser(name).parse(args).map {
      case Options(init, maybePort) =>
        LocationServerStatus.requireUpLocally()

        val wiring = ServerWiring.make(maybePort)
        import wiring._

        def shutdownAndLog(ex: Exception) = {
          Await.result(actorRuntime.shutdown(FailureReason(ex)), 10.seconds)
          log.error(ex.getMessage, ex = ex)
          throw ex
        }

        if (startLogging) actorRuntime.startLogging(name)
        if (init) svnRepo.initSvnRepo()

        try {
          svnRepo.testConnection()                                    // first test if the svn repo can be accessed successfully
          Await.result(httpService.registeredLazyBinding, 15.seconds) // then start the config server and register it with location service
          httpService
        } catch {
          case ex: SVNException ⇒
            shutdownAndLog(new RuntimeException(s"Could not open repository located at : ${settings.svnUrl}", ex))
          case ex: AASResolutionFailed ⇒ shutdownAndLog(ex)
        }
    }

}
