package csw.services

import com.typesafe.config.ConfigFactory
import csw.network.utils.Networks

case class Settings(
    clusterPort: String,
    locationHttpPort: String,
    configPort: String,
    dbUnixSocketDirs: String,
    dbPort: String,
    eventPort: String,
    alarmPort: String,
    sentinelPort: String,
    logHome: String,
    interfaceName: String
) {
  val hostName: String = Networks(Some(interfaceName)).hostname
}

object Settings {
  def apply(maybeInterface: Option[String] = None): Settings = {
    val config = ConfigFactory.load().getConfig("csw")

    // automatically determine correct interface if INTERFACE_NAME env variable not set or -i command line option not provided
    val interfaceName = maybeInterface.getOrElse((sys.env ++ sys.props).getOrElse("INTERFACE_NAME", ""))

    new Settings(
      config.getString("clusterPort"),
      config.getString("locationHttpPort"),
      config.getString("configPort"),
      config.getString("dbUnixSocketDir"),
      config.getString("dbPort"),
      config.getString("eventPort"),
      config.getString("alarmPort"),
      config.getString("sentinelPort"),
      config.getString("logHome"),
      interfaceName
    )
  }
}
