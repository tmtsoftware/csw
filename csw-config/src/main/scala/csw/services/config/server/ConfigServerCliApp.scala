package csw.services.config.server

import akka.Done
import csw.services.config.server.cli.ConfiServiceCliOptions

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future}

class ConfigServerCliApp {

  private val wiring = new ServerWiring
  import wiring._

  def start(args: Array[String]): Unit =
    configServerCliParser.parse(args).foreach {
      case ConfiServiceCliOptions(init, maybePort, clusterSeeds) =>
        sys.props("clusterSeeds") = clusterSeeds
        maybePort.foreach { port =>
          sys.props("httpPort") = port.toString
        }

        if (init) {
          wiring.svnRepo.initSvnRepo()
        }

        cswCluster.coordinatedShutdown.addJvmShutdownHook {
          Await.result(shutdown(), 10.seconds)
        }

        Await.result(httpService.lazyBinding, 5.seconds)
    }

  def shutdown(): Future[Done] = httpService.shutdown()
}

object ConfigServerCliApp extends App {
  new ConfigServerCliApp().start(args)
}
