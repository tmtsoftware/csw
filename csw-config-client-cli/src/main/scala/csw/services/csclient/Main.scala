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

object Main {
  def main(args: Array[String]): Unit = new Main(ClusterAwareSettings).start(args)
}
