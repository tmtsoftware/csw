package csw.services.csclient

import csw.services.csclient.cli.{ArgsParser, ClientCliWiring}
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

class Main(clusterSettings: ClusterSettings) {
  def start(args: Array[String]): Unit =
    ArgsParser.parse(args).foreach { options ⇒
      val wiring = new ClientCliWiring(clusterSettings)
      import wiring._
      try {
        commandLineRunner.run(options)
      } finally {
        Await.result(actorRuntime.shutdown(), 5.seconds)
      }
    }
}

object Main extends App {
  if (ClusterAwareSettings.seedNodes.isEmpty) {
    println(
      s"[error] clusterSeeds setting is not specified either as env variable or system property. Please check online documentation for this set-up."
    )
  } else {
    new Main(ClusterAwareSettings).start(args)
  }
}
