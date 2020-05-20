package csw.location.server.cli

import csw.location.api.models.NetworkType
import csw.network.utils.Networks

case class Options(
    clusterPort: Option[Int] = None,
    publicNetwork: Boolean = false
) {
  val httpBindHost: String = if (publicNetwork) Networks(NetworkType.Public.envKey).hostname else "127.0.0.1"
}
