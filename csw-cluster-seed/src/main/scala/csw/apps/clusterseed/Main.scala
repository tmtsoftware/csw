package csw.apps.clusterseed

import com.typesafe.scalalogging.LazyLogging
import csw.apps.clusterseed.cli.{ArgsParser, Options}
import csw.services.config.server.AdminWiring
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class Main(clusterSettings: ClusterSettings) {
  def start(args: Array[String]): Unit =
    new ArgsParser().parse(args).foreach {
      case Options(port) =>
        val updatedClusterSettings = clusterSettings.onPort(port)
        updatedClusterSettings.debug()
        val adminWiring = AdminWiring.make(updatedClusterSettings)
        adminWiring.cswCluster
        Await.result(adminWiring.adminHttpService.registeredLazyBinding, 5.seconds)
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
