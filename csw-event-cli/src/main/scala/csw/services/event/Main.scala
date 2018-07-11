package csw.services.event

import csw.messages.commons.CoordinatedShutdownReasons.ApplicationFinishedReason
import csw.services.BuildInfo
import csw.services.event.cli.args.{ArgsParser, Options}
import csw.services.event.cli.wiring.Wiring
import csw.services.location.commons.{ActorSystemFactory, ClusterAwareSettings}
import csw.services.logging.scaladsl.LoggingSystemFactory

// $COVERAGE-OFF$
object Main extends App {
  private val name = "csw-event-cli"

  if (ClusterAwareSettings.seedNodes.isEmpty) {
    println(
      "clusterSeeds setting is not specified either as env variable or system property. Please check online documentation for this set-up."
    )
  } else {
    new ArgsParser(name).parse(args) match {
      case Some(options) ⇒ run(options)
      case None          ⇒
    }
  }

  private def run(options: Options): Unit = {
    val actorSystem = ActorSystemFactory.remote()
    LoggingSystemFactory.start(name, BuildInfo.version, ClusterAwareSettings.hostname, actorSystem)

    val wiring = new Wiring(actorSystem)
    try {
      wiring.cliApp.start(options)
    } finally {
      wiring.actorRuntime.shutdown(ApplicationFinishedReason)
    }
  }
}
// $COVERAGE-ON$
