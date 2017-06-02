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
      case Options(clusterPort, maybeAdminPort) =>
        val wiring = AdminWiring.make(clusterSettings, clusterPort, maybeAdminPort)

        if (startLogging) wiring.actorRuntime.startLogging()

        clusterSettings.logDebugString()
        wiring.locationService
        Await.result(wiring.adminHttpService.registeredLazyBinding, 10.seconds)
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
