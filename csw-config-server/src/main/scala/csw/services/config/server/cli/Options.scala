package csw.services.config.server.cli

/**
 * Command line options to start a config server
 *
 * @param initRepo A boolean value stating whether to initialize repository on server or not.
 * @param port An optional port dictating where to start config server. If not provided, it is picked up from config file.
 */
case class Options(
    initRepo: Boolean = false,
    port: Option[Int] = None
)
