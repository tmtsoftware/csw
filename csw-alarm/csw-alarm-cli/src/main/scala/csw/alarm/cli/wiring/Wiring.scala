package csw.alarm.cli.wiring
import akka.actor.ActorSystem
import csw.alarm.cli.{CliApp, CommandLineRunner}
import csw.config.client.commons.ConfigUtils
import csw.config.client.scaladsl.ConfigClientFactory
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory

private[alarm] class Wiring {
  lazy val actorSystem  = ActorSystem("alarm-cli")
  lazy val actorRuntime = new ActorRuntime(actorSystem)
  import actorRuntime._

  lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient
  lazy val configClientService              = ConfigClientFactory.clientApi(system, locationService)
  lazy val configUtils                      = new ConfigUtils(configClientService)(system, mat)
  lazy val printLine: Any ⇒ Unit            = println
  lazy val commandLineRunner                = new CommandLineRunner(actorRuntime, locationService, configUtils, printLine)
  lazy val cliApp                           = new CliApp(commandLineRunner)
}

object Wiring {

  private[alarm] def make(locationHost: String = "localhost", _printLine: Any ⇒ Unit = println): Wiring =
    new Wiring {
      override lazy val locationService: LocationService =
        HttpLocationServiceFactory.make(locationHost)(actorSystem, actorRuntime.mat)

      override lazy val printLine: Any ⇒ Unit = _printLine
    }

}
