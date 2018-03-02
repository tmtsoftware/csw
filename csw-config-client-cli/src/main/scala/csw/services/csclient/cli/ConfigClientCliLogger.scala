package csw.services.csclient.cli

import csw.services.logging.scaladsl.LoggerFactory

/**
 * All the logs generated from config client cli will have a fixed componentName, which is `config-client-cli`.
 * The componentName helps in production to filter out logs from a particular component and this case, it helps to filter out logs
 * generated from config client cli.
 */
private[csclient] object ConfigClientCliLogger extends LoggerFactory("config-client-cli")
