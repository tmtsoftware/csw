package csw.services.config.client.scaladsl

import akka.actor.ActorSystem
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.client.internal.{ActorRuntime, ConfigClient, ConfigServiceResolver}
import csw.services.location.scaladsl.LocationService

object ConfigClientFactory {
  def make(actorSystem: ActorSystem, locationService: LocationService): ConfigService = {
    val actorRuntime          = new ActorRuntime(actorSystem)
    val configServiceResolver = new ConfigServiceResolver(locationService, actorRuntime)
    new ConfigClient(configServiceResolver, actorRuntime)
  }
}
