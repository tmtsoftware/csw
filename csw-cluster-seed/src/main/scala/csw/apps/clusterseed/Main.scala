package csw.apps.clusterseed

import csw.apps.clusterseed.admin.internal.AdminWiring
import csw.apps.clusterseed.cli.{ArgsParser, Options}
import csw.apps.clusterseed.commons.ClusterSeedLogger
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class Main(clusterSettings: ClusterSettings, startLogging: Boolean = false) extends ClusterSeedLogger.Simple {
  def start(args: Array[String]): Unit =
    new ArgsParser().parse(args).map {
      case Options(port) =>
        val updatedClusterSettings = clusterSettings.onPort(port)
        val wiring                 = new AdminWiring(updatedClusterSettings.system)
        if (startLogging) wiring.actorRuntime.startLogging()
        wiring.locationService
        updatedClusterSettings.logDebugString()
        Await.result(wiring.adminHttpService.registeredLazyBinding, 5.seconds)
    }
}

object Main extends App {
  if (ClusterAwareSettings.seedNodes.isEmpty) {
    println(
      "clusterSeeds setting is not specified either as env variable or system property. Please check online documentation for this set-up."
    )
  } else {
    new Main(ClusterAwareSettings, startLogging = true).start(args)
  }
}
