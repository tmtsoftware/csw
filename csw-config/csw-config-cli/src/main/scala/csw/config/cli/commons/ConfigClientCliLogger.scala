package csw.config.cli.commons

import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.CSW

/**
 * All the logs generated from config client cli will have a fixed prefix, which is `config-client-cli`.
 * The prefix helps in production to filter out logs from a particular component and this case, it helps to filter out logs
 * generated from config client cli.
 */
private[config] object ConfigClientCliLogger extends LoggerFactory(Prefix(CSW, "config_client_cli"))
