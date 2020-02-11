package csw.location.server.cli

case class Options(clusterPort: Option[Int] = None, public: Boolean = false) {
  val httpBindHost: String = if (public) "0.0.0.0" else "127.0.0.1"
}
