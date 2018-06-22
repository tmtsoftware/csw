package csw.apps.clusterseed

import csw.apps.clusterseed.internal.AdminWiring
import csw.apps.clusterseed.cli.{ArgsParser, Options}
import csw.services.location.commons.ClusterAwareSettings

/**
 * responsible for starting following:
 *  1. location service on provided port (this is required to bootstrap akka cluster, initially cluster will have single seed node)
 *  2. http server which exposes http end point to change/get the log level of components dynamically
 *
 * */
// $COVERAGE-OFF$
object Main extends App {
  private val name = "csw-cluster-seed"

  new ArgsParser(name).parse(args).map {
    case Options(maybeClusterPort, maybeAdminPort, testMode) =>
      if (!testMode && ClusterAwareSettings.seedNodes.isEmpty) {
        println(
          "[ERROR] clusterSeeds setting is not specified either as env variable or system property. Please check online documentation for this set-up."
        )
      } else {
        val wiring = AdminWiring.make(maybeClusterPort, maybeAdminPort)
        import wiring._
        actorRuntime.startLogging(name)

        locationHttpService.start()
        adminHttpService.registeredLazyBinding
      }
  }
}
// $COVERAGE-ON$
