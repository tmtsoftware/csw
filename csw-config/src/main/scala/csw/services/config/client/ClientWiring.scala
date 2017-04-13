package csw.services.config.client

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import csw.services.config.api.scaladsl.ConfigService
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}

class ClientWiring {
  lazy val config: Config = ConfigFactory.load()
  lazy val actorSystem = ActorSystem("config-client", config)
  lazy val actorRuntime = new ActorRuntime(actorSystem)

  lazy val locationService: LocationService = LocationServiceFactory.make()
  lazy val locationResolver = new LocationResolver(locationService)

  lazy val configService: ConfigService = new ConfigClient(locationResolver.configServiceLocation, actorRuntime)
}
