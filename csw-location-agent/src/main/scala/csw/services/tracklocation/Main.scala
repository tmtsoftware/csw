package csw.services.tracklocation

import akka.Done
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings, CswCluster}
import csw.services.tracklocation.models.Command
import csw.services.tracklocation.utils.ArgsParser

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

/**
 * Application object allowing program execution from command line, also facilitates an entry point for Component level testing.
 */
class Main(clusterSettings: ClusterSettings) {
  def start(args: Array[String]): Option[Done] =
    ArgsParser.parse(args).map { options =>
      val cswCluster = CswCluster.withSettings(clusterSettings)
      try {
        val command = Command.parse(options)
        println(s"commandText: ${command.commandText}, command: $command")
        val trackLocation = new TrackLocation(options.names, command, cswCluster)
        trackLocation.run()
      } finally {
        Await.result(cswCluster.shutdown(), 10.seconds)
      }
    }
}

object Main extends App {
  if (ClusterAwareSettings.seedNodes.isEmpty) {
    println(
      s"[error] clusterSeeds setting is not configured. Please do so by either setting the env variable or system property."
    )
  } else {
    new Main(ClusterAwareSettings).start(args)
  }
}
