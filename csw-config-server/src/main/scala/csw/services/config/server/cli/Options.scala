package csw.services.config.server.cli

// Command line options
case class Options(
    init: Boolean = false,
    port: Option[Int] = None
)
