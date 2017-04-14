package csw.services.config.client.internal

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import csw.services.config.api.scaladsl.ConfigService
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}

class ClientWiring {
  lazy val config: Config = ConfigFactory.load()
  lazy val actorSystem = ActorSystem("config-client", config)
  lazy val actorRuntime = new ActorRuntime(actorSystem)

  lazy val locationService: LocationService = LocationServiceFactory.make()
  lazy val configServiceResolver = new ConfigServiceResolver(locationService, actorRuntime)

  lazy val configService: ConfigService = new ConfigClient(configServiceResolver, actorRuntime)
}

object ClientWiring {

  def make(_actorSystem: ActorSystem): ClientWiring = new ClientWiring {
    override lazy val actorSystem: ActorSystem = _actorSystem
  }

  def make(_locationService: LocationService): ClientWiring = new ClientWiring {
    override lazy val locationService: LocationService = _locationService
  }

  def make(_actorSystem: ActorSystem, _locationService: LocationService): ClientWiring = new ClientWiring {
    override lazy val actorSystem: ActorSystem = _actorSystem
    override lazy val locationService: LocationService = _locationService
  }

}
