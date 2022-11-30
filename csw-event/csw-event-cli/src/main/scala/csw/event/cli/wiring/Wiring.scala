/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.cli.wiring

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.event.api.scaladsl.EventService
import csw.event.cli.{CliApp, CommandLineRunner}
import csw.event.client.EventServiceFactory
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory

private[event] class Wiring {
  lazy val actorSystem                      = ActorSystem(SpawnProtocol(), "event-cli")
  lazy val actorRuntime                     = new ActorRuntime(actorSystem)
  lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient(actorSystem)
  lazy val eventService: EventService       = new EventServiceFactory().make(locationService)(actorSystem)
  lazy val printLine: Any => Unit           = println
  lazy val commandLineRunner                = new CommandLineRunner(eventService, actorRuntime, printLine)
  lazy val cliApp                           = new CliApp(commandLineRunner)(actorSystem)
}

object Wiring {

  private[event] def make(locationHost: String = "localhost", _printLine: Any => Unit = println): Wiring =
    new Wiring {
      override lazy val locationService: LocationService =
        HttpLocationServiceFactory.make(locationHost)(actorSystem)

      override lazy val printLine: Any => Unit = _printLine
    }

}
