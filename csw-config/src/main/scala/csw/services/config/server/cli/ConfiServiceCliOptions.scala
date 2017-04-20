package csw.services.config.server.cli

// Command line options
case class ConfiServiceCliOptions(
    init: Boolean = false,
    port: Option[Int] = None,
    clusterSeeds: String = ""
)
