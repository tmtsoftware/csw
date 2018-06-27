package csw.services.event.cli
import akka.actor.ActorSystem
import csw.services.event.internal.redis.RedisEventServiceFactory
import csw.services.event.scaladsl.EventService
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}

class Wiring(actorSystem: ActorSystem) {
  lazy val actorRuntime = new ActorRuntime(actorSystem)
  import actorRuntime._
  lazy val locationService: LocationService = LocationServiceFactory.makeRemoteHttpClient
  lazy val eventService: EventService       = new RedisEventServiceFactory().make(locationService)
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
