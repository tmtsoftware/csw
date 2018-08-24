package csw.services.alarm
import akka.http.scaladsl.Http
import csw.messages.commons.CoordinatedShutdownReasons.ApplicationFinishedReason
import csw.services.BuildInfo
import csw.services.alarm.cli.args.{ArgsParser, Options}
import csw.services.alarm.cli.wiring.Wiring
import csw.services.location.commons.{ActorSystemFactory, ClusterAwareSettings}
import csw.services.logging.scaladsl.LoggingSystemFactory

// $COVERAGE-OFF$
object Main extends App {
  private val name: String = BuildInfo.name

  if (ClusterAwareSettings.seedNodes.isEmpty)
    println(
      "clusterSeeds setting is not specified either as env variable or system property. Please check online documentation for this set-up."
    )
  else new ArgsParser(name).parse(args).foreach(run)

  private def run(options: Options): Unit = {
    val actorSystem = ActorSystemFactory.remote()
    LoggingSystemFactory.start(name, BuildInfo.version, ClusterAwareSettings.hostname, actorSystem)

    val wiring = new Wiring(actorSystem)
    import wiring._
    import actorRuntime._

    try commandExecutor.execute(options)
    finally Http().shutdownAllConnectionPools().onComplete(_ ⇒ shutdown(ApplicationFinishedReason))
  }
}
// $COVERAGE-ON$
