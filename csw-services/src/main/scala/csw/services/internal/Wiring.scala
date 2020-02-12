package csw.services.internal

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.services.{AuthServer, LocationAgent, Redis}

import scala.concurrent.ExecutionContext

class Wiring(maybeInterface: Option[String]) {
  lazy implicit val actorSystem: ActorSystem[Nothing] = ActorSystemFactory.remote(Behaviors.empty)
  lazy implicit val ec: ExecutionContext              = actorSystem.executionContext

  lazy val settings: Settings               = Settings(maybeInterface)
  lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  lazy val environment   = new Environment(settings)
  lazy val locationAgent = new LocationAgent(settings)
  lazy val redis         = new Redis(settings)
  lazy val keycloak      = new AuthServer(locationService, settings)
}
