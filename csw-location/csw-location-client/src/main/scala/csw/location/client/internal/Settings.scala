package csw.location.client.internal
import com.typesafe.config.{Config, ConfigFactory}

class Settings(config: Config) {
  private val locationConfig = config.getConfig("csw-location-client")
  val serverPort: Int        = locationConfig.getInt("server-http-port")
}

object Settings {
  def apply(config: Config = ConfigFactory.load()): Settings = new Settings(config)
}
