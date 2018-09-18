package csw.event.cli.wiring

import akka.actor.ActorSystem
import csw.event.api.scaladsl.EventService
import csw.event.cli.{CliApp, CommandLineRunner}
import csw.event.client.EventServiceFactory
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory

class Wiring(actorSystem: ActorSystem) {
  lazy val actorRuntime = new ActorRuntime(actorSystem)
  import actorRuntime._
  lazy val locationService: LocationService = HttpLocationServiceFactory.makeRemoteHttpClient
  lazy val eventService: EventService       = new EventServiceFactory().make(locationService)
  lazy val printLine: Any ⇒ Unit            = println
  lazy val commandLineRunner                = new CommandLineRunner(eventService, actorRuntime, printLine)
  lazy val cliApp                           = new CliApp(commandLineRunner)
}

object Wiring {
  private[event] def make(_actorSystem: ActorSystem, _locationService: LocationService, _printLine: Any ⇒ Unit): Wiring =
    new Wiring(_actorSystem) {
      override lazy val locationService: LocationService = _locationService
      override lazy val printLine: Any ⇒ Unit            = _printLine
    }
}
