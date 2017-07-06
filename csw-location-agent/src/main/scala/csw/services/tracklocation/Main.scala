package csw.services.tracklocation

import csw.services.BuildInfo
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}
import csw.services.logging.appenders.{FileAppender, StdOutAppender}
import csw.services.logging.scaladsl.LoggingSystemFactory
import csw.services.tracklocation.commons.LocationAgentLogger
import csw.services.tracklocation.models.Command
import csw.services.tracklocation.utils.ArgsParser

import scala.sys.process.Process

/**
 * Application object allowing program execution from command line, also facilitates an entry point for Component level testing.
 */
class Main(clusterSettings: ClusterSettings, startLogging: Boolean = false) extends LocationAgentLogger.Simple {
  def start(args: Array[String]): Option[Process] =
    ArgsParser.parse(args).map { options =>
      val actorSystem = clusterSettings.system

      if (startLogging)
        LoggingSystemFactory.start(BuildInfo.name, BuildInfo.version, clusterSettings.hostname, actorSystem,
          Seq(StdOutAppender, FileAppender))

      val command = Command.parse(options)
      log.info(s"commandText: ${command.commandText}, command: ${command.toString}")
      val trackLocation = new TrackLocation(options.names, command, actorSystem)
      trackLocation.run()
    }
}

object Main extends App with LocationAgentLogger.Simple {
  if (ClusterAwareSettings.seedNodes.isEmpty) {
    println(
      "clusterSeeds setting is not specified either as env variable or system property. Please check online documentation for this set-up."
    )
  } else {
    new Main(ClusterAwareSettings, startLogging = true).start(args)
  }
}
