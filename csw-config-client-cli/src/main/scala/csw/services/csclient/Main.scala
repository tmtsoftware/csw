package csw.services.csclient

import csw.services.BuildInfo
import csw.services.csclient.cli.{ArgsParser, ClientCliWiring, Options}
import csw.services.location.commons.ClusterAwareSettings
import csw.services.logging.appenders.FileAppender
import csw.services.logging.scaladsl.LoggingSystem

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

object Main extends App {
  if (ClusterAwareSettings.seedNodes.isEmpty) {
    println(
      "clusterSeeds setting is not specified either as env variable or system property. Please check online documentation for this set-up."
    )
  } else {
    ArgsParser.parse(args) match {
      case None          ⇒
      case Some(options) ⇒ run(options)
    }
  }

  private def run(options: Options): Unit = {
    val actorSystem = ClusterAwareSettings.system
    new LoggingSystem(BuildInfo.name, BuildInfo.version, ClusterAwareSettings.hostname, actorSystem, Seq(FileAppender))

    val wiring = new ClientCliWiring(actorSystem)
    try {
      wiring.cliApp.start(options)
    } finally {
      Await.result(wiring.actorRuntime.shutdown(), 15.seconds)
    }
  }
}
