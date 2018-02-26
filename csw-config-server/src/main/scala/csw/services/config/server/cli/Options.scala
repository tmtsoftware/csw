package csw.services.config.server.cli

// Command line options
case class Options(
    initRepo: Boolean = false,
    port: Option[Int] = None
) //TODO: update doc
