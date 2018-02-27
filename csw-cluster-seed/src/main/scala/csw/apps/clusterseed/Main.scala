package csw.apps.clusterseed

import csw.apps.clusterseed.admin.internal.AdminWiring
import csw.apps.clusterseed.cli.{ArgsParser, Options}
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

// $COVERAGE-OFF$
class Main(clusterSettings: ClusterSettings, startLogging: Boolean = false) {
  //TODO: add doc to explain working
  def start(args: Array[String]): Unit =
    new ArgsParser().parse(args).map {
      case Options(clusterPort, maybeAdminPort) =>
        val updatedClusterSettings = clusterSettings.onPort(clusterPort)
        val wiring                 = AdminWiring.make(updatedClusterSettings, maybeAdminPort)

        if (startLogging) wiring.actorRuntime.startLogging()

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
// $COVERAGE-ON$
