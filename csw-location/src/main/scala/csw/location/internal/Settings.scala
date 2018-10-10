package csw.location.internal

import com.typesafe.config.Config

class Settings(config: Config) {

  private val clusterSeedConfig = config.getConfig("csw-location")

  def clusterPort: Int = clusterSeedConfig.getInt("cluster-port")
  def httpPort: Int    = clusterSeedConfig.getInt("http-port")
}
