package csw.event.cli

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import csw.event.cli.args.{ArgsParser, Options}
import csw.event.cli.commons.ApplicationFinishedReason
import csw.event.cli.wiring.Wiring
import csw.location.api.commons.ClusterAwareSettings
import csw.logging.scaladsl.LoggingSystemFactory
import csw.services.BuildInfo

// $COVERAGE-OFF$
object Main extends App {
  private val name = BuildInfo.name

  if (ClusterAwareSettings.seedNodes.isEmpty)
    println(
      "clusterSeeds setting is not specified either as env variable or system property. Please check online documentation for this set-up."
    )
  else
    new ArgsParser(name).parse(args) match {
      case Some(options) ⇒ run(options)
      case None          ⇒
    }

  private def run(options: Options): Unit = {
    val actorSystem = ActorSystem()
    LoggingSystemFactory.start(name, BuildInfo.version, ClusterAwareSettings.hostname, actorSystem)

    val wiring = new Wiring(actorSystem)
    import wiring._
    import actorRuntime._

    try cliApp.start(options)
    finally Http().shutdownAllConnectionPools().onComplete(_ ⇒ actorRuntime.shutdown(ApplicationFinishedReason))
  }
}
// $COVERAGE-ON$
