package csw.services.alarm
import csw.services.BuildInfo
import csw.services.alarm.cli.args.{ArgsParser, CommandLineArgs}
import csw.services.alarm.cli.wiring.Wiring
import csw.services.location.commons.{ActorSystemFactory, ClusterAwareSettings}
import csw.services.logging.scaladsl.LoggingSystemFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

// $COVERAGE-OFF$
object Main extends App {
  private val name: String = BuildInfo.name

  if (ClusterAwareSettings.seedNodes.isEmpty)
    println(
      "clusterSeeds setting is not specified either as env variable or system property. Please check online documentation for this set-up."
    )
  else
    new ArgsParser(name)
      .parse(args)
      .foreach(run)

  private def run(commandLineArgs: CommandLineArgs): Unit = {
    val actorSystem = ActorSystemFactory.remote()
    LoggingSystemFactory.start(name, BuildInfo.version, ClusterAwareSettings.hostname, actorSystem)

    val wiring = new Wiring(actorSystem)
    import wiring._

    commandExecutor.execute(commandLineArgs)
  }
}
// $COVERAGE-ON$
