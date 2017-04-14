package csw.services.config.client.internal

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import csw.services.config.api.javadsl.IConfigService
import csw.services.config.api.scaladsl.ConfigService
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}

class ClientWiring {
  lazy val config: Config = ConfigFactory.load()
  lazy val actorSystem = ActorSystem("config-client", config)
  lazy val actorRuntime = new ActorRuntime(actorSystem)

  lazy val locationService: LocationService = LocationServiceFactory.make()
  lazy val configServiceResolver = new ConfigServiceResolver(locationService, actorRuntime)

  lazy val configService: ConfigService = new ConfigClient(configServiceResolver, actorRuntime)
  lazy val javaConfigService: IConfigService = new JConfigService(configService, actorRuntime)
}

object ClientWiring {
  def make(): ClientWiring = new ClientWiring

  def make(_actorSystem: ActorSystem): ClientWiring = new ClientWiring {
    override lazy val actorSystem: ActorSystem = _actorSystem
  }
}
