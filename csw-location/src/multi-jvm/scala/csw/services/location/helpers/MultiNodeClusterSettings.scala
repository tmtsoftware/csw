package csw.services.location.helpers

import csw.services.location.commons.ClusterSettings

class MultiNodeClusterSettings extends ClusterSettings {

  val envProperties = sys.env ++ sys.props

  override def joinLocal(port: Int, ports: Int*) = {
    val clusterSeeds = envProperties.get(ClusterSeedsKey)
    val seeds        = s"$hostname:$port" +: ports.map(port ⇒ s"$hostname:$port")

    if (clusterSeeds.isDefined) joinSeeds(clusterSeeds.get)
    else joinSeeds(seeds.mkString(","))
  }

}
