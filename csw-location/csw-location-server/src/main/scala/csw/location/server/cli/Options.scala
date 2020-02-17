package csw.location.server.cli

case class Options(
    clusterPort: Option[Int] = None,
    publicNetwork: Boolean = false
) {
  val httpBindHost: String = if (publicNetwork) "0.0.0.0" else "127.0.0.1"
}
