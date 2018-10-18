package csw.config.cli

import akka.http.scaladsl.Http
import csw.config.cli.args.{ArgsParser, Options}
import csw.config.cli.commons.CoordinatedShutdownReasons.ApplicationFinishedReason
import csw.config.cli.wiring.Wiring
import csw.location.api.commons.ClusterAwareSettings
import csw.location.client.utils.LocationServerStatus
import csw.logging.scaladsl.LoggingSystemFactory
import csw.services.BuildInfo

// $COVERAGE-OFF$
object Main extends App {
  private val name = BuildInfo.name

  new ArgsParser(name).parse(args).foreach(run)

  private def run(options: Options): Unit = {
    LocationServerStatus.requireUp(options.locationHost)

    val wiring = Wiring.make(options.locationHost)
    import wiring._
    LoggingSystemFactory.start(name, BuildInfo.version, ClusterAwareSettings.hostname, actorSystem)

    try {
      cliApp.start(options)
    } finally {
      Http(actorSystem)
        .shutdownAllConnectionPools()
        .foreach(_ => actorRuntime.shutdown(ApplicationFinishedReason))(actorRuntime.ec)
    }
  }
}
// $COVERAGE-ON$
