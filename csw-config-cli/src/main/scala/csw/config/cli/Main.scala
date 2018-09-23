package csw.config.cli

import csw.services.BuildInfo
import csw.config.cli.commons.CoordinatedShutdownReasons.ApplicationFinishedReason
import csw.location.api.commons.ClusterAwareSettings
import csw.logging.scaladsl.LoggingSystemFactory

// $COVERAGE-OFF$
object Main extends App {
  private val name = BuildInfo.name

  if (ClusterAwareSettings.seedNodes.isEmpty) {
    println(
      "clusterSeeds setting is not specified either as env variable or system property. Please check online documentation for this set-up."
    )
  } else {
    new ArgsParser(name).parse(args) match {
      case None          ⇒
      case Some(options) ⇒ run(options)
    }
  }

  private def run(options: Options): Unit = {
    val actorSystem = ClusterAwareSettings.system
    LoggingSystemFactory.start(name, BuildInfo.version, ClusterAwareSettings.hostname, actorSystem)

    val wiring = new ClientCliWiring(actorSystem)
    try {
      wiring.cliApp.start(options)
    } finally {
      wiring.actorRuntime.shutdown(ApplicationFinishedReason)
    }
  }
}
// $COVERAGE-ON$
