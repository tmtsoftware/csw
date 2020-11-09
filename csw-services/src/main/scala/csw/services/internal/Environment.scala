package csw.services.internal

class Environment(settings: Settings) {
  def setup(): Unit = {
    import settings._
    System.setProperty("CLUSTER_SEEDS", s"$hostName:$clusterPort")
    System.setProperty("csw-location-server.http-port", locationHttpPort)
    System.setProperty("INTERFACE_NAME", insideInterfaceName)
    System.setProperty("AAS_INTERFACE_NAME", outsideInterfaceName)
    System.setProperty("TMT_LOG_HOME", logHome)
  }
}
