package csw.config.server.cli

/**
 * Command line options to start a config server
 *
 * @param initRepo a boolean value stating whether to initialize repository on server or not.
 * @param port an optional port dictating where to start config server. If not provided, it is picked up from config file.
 */
case class Options(initRepo: Boolean = false, port: Option[Int] = None)
