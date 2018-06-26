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
