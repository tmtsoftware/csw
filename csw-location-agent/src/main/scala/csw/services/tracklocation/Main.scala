package csw.services.tracklocation

import com.typesafe.scalalogging.LazyLogging
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}
import csw.services.tracklocation.models.Command
import csw.services.tracklocation.utils.ArgsParser

import scala.sys.process.Process

/**
 * Application object allowing program execution from command line, also facilitates an entry point for Component level testing.
 */
class Main(clusterSettings: ClusterSettings) extends LazyLogging {
  def start(args: Array[String]): Option[Process] =
    ArgsParser.parse(args).map { options =>
      val command = Command.parse(options)
      logger.info(s"commandText: ${command.commandText}, command: $command")
      val trackLocation = new TrackLocation(options.names, command, clusterSettings)
      trackLocation.run()
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
