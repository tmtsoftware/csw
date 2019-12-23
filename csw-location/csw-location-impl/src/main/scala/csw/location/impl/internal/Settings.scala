package csw.location.impl.internal

import com.typesafe.config.ConfigFactory

case class Settings(clusterPort: Int, httpPort: Int) {
  def withHttpPort(httpPortMaybe: Option[Int]): Settings       = copy(httpPort = httpPortMaybe.getOrElse(httpPort))
  def withClusterPort(clusterPortMaybe: Option[Int]): Settings = copy(clusterPort = clusterPortMaybe.getOrElse(clusterPort))
}

object Settings {
  def apply(configKey: String): Settings = {
    val config      = ConfigFactory.load().getConfig(configKey)
    val clusterPort = config.getInt("cluster-port")
    val httpPort    = config.getInt("http-port")
    Settings(clusterPort, httpPort)
  }
}
