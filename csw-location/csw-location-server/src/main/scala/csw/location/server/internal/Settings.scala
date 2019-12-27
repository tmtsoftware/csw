package csw.location.server.internal

import com.typesafe.config.Config

class Settings(config: Config) {

  private val locationServerConfig = config.getConfig("csw-location-server")

  def clusterPort: Int = locationServerConfig.getInt("cluster-port")
  def httpPort: Int    = locationServerConfig.getInt("http-port")
}
