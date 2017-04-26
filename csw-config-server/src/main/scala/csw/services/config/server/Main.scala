package csw.services.config.server

import csw.services.config.server.cli.{ArgsParser, Options}
import csw.services.config.server.http.HttpService
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}

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

        cswCluster.addJvmShutdownHook {
          Await.result(httpService.shutdown(), 10.seconds)
        }

        Await.result(httpService.registeredLazyBinding, 5.seconds)
        httpService
    }
}

object Main extends App {
  if (ClusterAwareSettings.seedNodes.isEmpty) {
    println(
      s"[error] clusterSeeds setting is not specified either as env variable or system property. Please check online documentation for this set-up."
    )
  } else {
    new Main(ClusterAwareSettings).start(args)
  }
}
