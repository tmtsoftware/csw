package csw.services.location_agent

import csw.services.BuildInfo
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}
import csw.services.logging.scaladsl.{Logger, LoggingSystemFactory}
import csw.services.location_agent.commons.LocationAgentLogger
import csw.services.location_agent.models.Command
import csw.services.location_agent.utils.ArgsParser

import scala.sys.process.Process

// $COVERAGE-OFF$
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

/**
 * Application object allowing program execution from command line, also facilitates an entry point for Component level testing.
 */
class Main(clusterSettings: ClusterSettings, startLogging: Boolean) {
  private val name        = "csw-location-agent"
  private val log: Logger = LocationAgentLogger.getLogger

  def start(args: Array[String]): Option[Process] =
    new ArgsParser(name).parse(args).map { options =>
      val actorSystem = clusterSettings.system

      if (startLogging)
        LoggingSystemFactory.start(name, BuildInfo.version, clusterSettings.hostname, actorSystem)

      val command = Command.parse(options)

      log.info(s"commandText: ${command.commandText}, command: ${command.toString}")
      val locationAgent = new LocationAgent(options.names, command, actorSystem)
      locationAgent.run()
    }
}
