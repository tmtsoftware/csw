/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.cli.wiring
import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import csw.alarm.cli.{CliApp, CommandLineRunner}
import csw.config.api.scaladsl.ConfigClientService
import csw.config.client.commons.ConfigUtils
import csw.config.client.scaladsl.ConfigClientFactory
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory

private[alarm] class Wiring {
  implicit lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "alarm-cli")
  val actorRuntime                                             = new ActorRuntime(actorSystem)
  lazy val locationService: LocationService                         = HttpLocationServiceFactory.makeLocalClient
  lazy val configClientService: ConfigClientService                 = ConfigClientFactory.clientApi(actorSystem, locationService)
  lazy val configUtils                                              = new ConfigUtils(configClientService)
  lazy val printLine: Any => Unit                                   = println
  val commandLineRunner = new CommandLineRunner(actorRuntime, locationService, configUtils, printLine)
  lazy val cliApp            = new CliApp(commandLineRunner)
}

object Wiring {

  private[alarm] def make(locationHost: String = "localhost", _printLine: Any => Unit = println): Wiring =
    new Wiring {
      override lazy val locationService: LocationService =
        HttpLocationServiceFactory.make(locationHost)

      override lazy val printLine: Any => Unit = _printLine
    }

}
