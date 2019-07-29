package csw.alarm.cli.wiring
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.alarm.cli.{CliApp, CommandLineRunner}
import csw.config.api.scaladsl.ConfigClientService
import csw.config.client.commons.ConfigUtils
import csw.config.client.scaladsl.ConfigClientFactory
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory

private[alarm] class Wiring {
  lazy val typedSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "alarm-cli")
  lazy val actorRuntime                                    = new ActorRuntime(typedSystem)
  import actorRuntime._

  lazy val locationService: LocationService         = HttpLocationServiceFactory.makeLocalClient(typedSystem, mat)
  lazy val configClientService: ConfigClientService = ConfigClientFactory.clientApi(typedSystem, locationService)
  lazy val configUtils                              = new ConfigUtils(configClientService)(typedSystem, mat)
  lazy val printLine: Any => Unit                   = println
  lazy val commandLineRunner                        = new CommandLineRunner(actorRuntime, locationService, configUtils, printLine)
  lazy val cliApp                                   = new CliApp(commandLineRunner)
}

object Wiring {

  private[alarm] def make(locationHost: String = "localhost", _printLine: Any => Unit = println): Wiring =
    new Wiring {
      override lazy val locationService: LocationService =
        HttpLocationServiceFactory.make(locationHost)(typedSystem, actorRuntime.mat)

      override lazy val printLine: Any => Unit = _printLine
    }

}
