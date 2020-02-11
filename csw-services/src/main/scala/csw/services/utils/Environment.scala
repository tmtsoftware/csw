package csw.services.utils

import csw.services.Settings

object Environment {
  def setup(settings: Settings): Unit = {
    import settings._
    System.setProperty("CLUSTER_SEEDS", s"$hostName:$clusterPort")
    System.setProperty("csw-location-server.http-port", locationHttpPort)
    System.setProperty("INTERFACE_NAME", interfaceName)
    System.setProperty("TMT_LOG_HOME", logHome)
    // fixme: this does not work!
    System.setProperty("PGDATA", "/usr/local/var/postgres")
  }
}
