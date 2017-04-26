package csw.services.csclient

import csw.services.csclient.cli.{ArgsParser, Wiring}
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}

class Main(clusterSettings: ClusterSettings) {
  def start(args: Array[String]): Unit =
    ArgsParser.parse(args).foreach { options ⇒
      val wiring = new Wiring(clusterSettings)
      try {
        wiring.commandLineRunner.run(options)
      } finally {
        wiring.shutdown()
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
