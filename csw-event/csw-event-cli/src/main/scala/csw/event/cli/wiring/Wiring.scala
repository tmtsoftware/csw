/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.cli.wiring

import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import csw.event.api.scaladsl.EventService
import csw.event.cli.{CliApp, CommandLineRunner}
import csw.event.client.EventServiceFactory
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory

private[event] class Wiring {
  val actorSystem  = ActorSystem(SpawnProtocol(), "event-cli")
  val actorRuntime = new ActorRuntime(actorSystem)
  import actorRuntime._
  lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient
  lazy val eventService: EventService       = new EventServiceFactory().make(locationService)
  lazy val printLine: Any => Unit           = println
  lazy val commandLineRunner                = new CommandLineRunner(eventService, actorRuntime, printLine)
  lazy val cliApp                           = new CliApp(commandLineRunner)
}

object Wiring {

  private[event] def make(locationHost: String = "localhost", _printLine: Any => Unit = println): Wiring =
    new Wiring {
      override lazy val locationService: LocationService =
        HttpLocationServiceFactory.make(locationHost)(actorSystem)

      override lazy val printLine: Any => Unit = _printLine
    }

}
