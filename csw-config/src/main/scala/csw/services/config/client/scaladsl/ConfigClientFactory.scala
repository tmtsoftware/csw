package csw.services.config.client.scaladsl

import akka.actor.ActorSystem
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.client.internal.ClientWiring
import csw.services.location.scaladsl.LocationService

object ConfigClientFactory {

  def make(): ConfigService = new ClientWiring().configService

  def make(actorSystem: ActorSystem): ConfigService = ClientWiring.make(actorSystem).configService

  def make(locationService: LocationService): ConfigService = ClientWiring.make(locationService).configService

  def make(actorSystem: ActorSystem, locationService: LocationService): ConfigService = {
    ClientWiring.make(actorSystem, locationService).configService
  }

}
