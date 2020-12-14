package csw.services.internal

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
    interfaceName: String,
    outsideInterfaceName: String,
    keycloakPort: String,
    configAdminUsername: String,
    configAdminPassword: String
) {
  val hostName: String = Networks.interface(Some(interfaceName)).hostname
}

object Settings {
  def apply(interface: Option[String] = None, outsideInterface: Option[String] = None): Settings = {
    val config = ConfigFactory.load().getConfig("csw")

    // automatically determine correct interface if INTERFACE_NAME env variable not set or -i command line option not provided
    val interfaceName        = interface.getOrElse((sys.env ++ sys.props).getOrElse("INTERFACE_NAME", ""))
    val outsideInterfaceName = outsideInterface.getOrElse(interfaceName)

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
      interfaceName,
      outsideInterfaceName,
      config.getString("keycloakPort"),
      config.getString("configAdminUsername"),
      config.getString("configAdminPassword")
    )
  }
}
