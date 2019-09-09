package csw.location.server

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.util.Timeout
import csw.location.server.cli.{ArgsParser, Options}
import csw.location.server.commons.ClusterAwareSettings
import csw.location.server.commons.CoordinatedShutdownReasons.FailureReason
import csw.location.server.dns.DNSService
import csw.location.server.internal.ServerWiring

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble
import scala.util.control.NonFatal

/**
 * responsible for starting following:
 *  1. location service on provided port (this is required to bootstrap akka cluster, initially cluster will have single seed node)
 *  2. http server which exposes http end point to change/get the log level of components dynamically
 *
 * */
// $COVERAGE-OFF$
object Main extends App {
  private val name = BuildInfo.name

  new ArgsParser(name).parse(args.toList).foreach {
    case Options(maybeClusterPort) =>
      if (ClusterAwareSettings.seedNodes.isEmpty) {
        println(
          "[ERROR] CLUSTER_SEEDS setting is not specified either as env variable or system property. Please check online documentation for this set-up."
        )
      } else {
        val wiring = ServerWiring.make(maybeClusterPort)

        import wiring._
        import actorRuntime._
        try {
          startLogging(name, clusterSettings.hostname)

          val locationBindingF = locationHttpService.start()

          implicit val timeout: Timeout = Timeout(2.seconds)
          Await.result(DNSService.start(53, locationService), timeout.duration)

          coordinatedShutdown.addTask(
            CoordinatedShutdown.PhaseServiceUnbind,
            "unbind-services"
          ) { () =>
            locationBindingF.flatMap(_.terminate(30.seconds)).map(_ => Done)
          }
        } catch {
          case NonFatal(ex) =>
            println(s"[ERROR] Failed to start location server.")
            ex.printStackTrace()
            shutdown(FailureReason(ex))
        }
      }
  }
}
