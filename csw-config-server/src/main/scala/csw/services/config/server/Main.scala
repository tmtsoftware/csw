package csw.services.config.server

import csw.services.config.server.cli.{ArgsParser, Options}
import csw.services.config.server.http.HttpService
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class Main(clusterSettings: ClusterSettings, args: Array[String]) {

  if (clusterSettings.seedNodes.isEmpty) {
    println(
      s"[error] clusterSeeds setting is not configured. Please do so by either setting the env variable or system property."
    )
    System.exit(1)
  }

  lazy val configServerCliParser = new ArgsParser

  lazy val maybeHttpService: Option[HttpService] =
    configServerCliParser.parse(args).map {
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
  new Main(ClusterAwareSettings, args).maybeHttpService
}
