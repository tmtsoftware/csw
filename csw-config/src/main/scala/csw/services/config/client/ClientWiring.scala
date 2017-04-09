package csw.services.config.client

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import csw.services.config.commons.ActorRuntime
import csw.services.config.scaladsl.ConfigManager
import csw.services.location.models.Connection.HttpConnection
import csw.services.location.models._

class ClientWiring {
  val config: Config = ConfigFactory.load()
  val actorSystem = ActorSystem("config-client", config)
  val actorRuntime = new ActorRuntime(actorSystem)

  val registration = HttpRegistration(HttpConnection(ComponentId("configClient", ComponentType.Service)), 4000, "")
  val location: Location = registration.location("localhost")
  val configManager: ConfigManager = new ConfigClient(location, actorRuntime)
}
