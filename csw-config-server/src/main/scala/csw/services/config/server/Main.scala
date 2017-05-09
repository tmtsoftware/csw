package csw.services.config.server

import com.typesafe.scalalogging.LazyLogging
import csw.services.config.server.cli.{ArgsParser, Options}
import csw.services.config.server.http.HttpService
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}
import org.tmatesoft.svn.core.SVNException

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class Main(clusterSettings: ClusterSettings) {
  def start(args: Array[String]): Option[HttpService] =
    new ArgsParser().parse(args).map {
      case Options(init, maybePort) =>
        clusterSettings.debug()
        val wiring = ServerWiring.make(clusterSettings, maybePort)
        import wiring._

        if (init) {
          svnRepo.initSvnRepo()
        }

        try {
          svnRepo.testConnection()
          Await.result(httpService.registeredLazyBinding, 5.seconds)
          httpService
        } catch {
          case ex: SVNException ⇒ {
            Await.result(actorRuntime.shutdown(), 10.seconds)
            throw new RuntimeException(s"Could not open repository located at : ${settings.svnUrl}", ex)
          }
        }

    }
}

object Main extends App with LazyLogging {
  if (ClusterAwareSettings.seedNodes.isEmpty) {
    logger.error(
      "clusterSeeds setting is not specified either as env variable or system property. Please check online documentation for this set-up."
    )
  } else {
    new Main(ClusterAwareSettings).start(args)
  }
}
