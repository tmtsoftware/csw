package csw.clusterseed

import akka.Done
import akka.actor.CoordinatedShutdown
import csw.clusterseed.internal.AdminWiring
import csw.clusterseed.cli.{ArgsParser, Options}
import csw.services.BuildInfo
import csw.location.api.commons.ClusterAwareSettings

import scala.concurrent.duration.DurationDouble

/**
 * responsible for starting following:
 *  1. location service on provided port (this is required to bootstrap akka cluster, initially cluster will have single seed node)
 *  2. http server which exposes http end point to change/get the log level of components dynamically
 *
 * */
// $COVERAGE-OFF$
object Main extends App {
  private val name = BuildInfo.name

  new ArgsParser(name).parse(args).foreach {
    case Options(maybeClusterPort, maybeAdminPort, testMode) =>
      if (!testMode && ClusterAwareSettings.seedNodes.isEmpty) {
        println(
          "[ERROR] clusterSeeds setting is not specified either as env variable or system property. Please check online documentation for this set-up."
        )
      } else {
        val wiring = AdminWiring.make(maybeClusterPort, maybeAdminPort)
        import wiring._
        import actorRuntime._
        startLogging(name)

        val locationBindingF = locationHttpService.start()
        val logAdminBindingF = adminHttpService.registeredLazyBinding

        coordinatedShutdown.addTask(
          CoordinatedShutdown.PhaseServiceUnbind,
          "unbind-services"
        ) { () ⇒
          val hardDeadline = 30.seconds
          locationBindingF.flatMap(_.terminate(hardDeadline)).map(_ ⇒ Done)
          logAdminBindingF.flatMap(_.terminate(hardDeadline)).map(_ ⇒ Done)
        }
      }
  }
}
// $COVERAGE-ON$
