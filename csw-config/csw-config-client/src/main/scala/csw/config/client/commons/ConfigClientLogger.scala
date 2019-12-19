package csw.config.client.commons

import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.CSW

/**
 * All the logs generated from config client will have a fixed prefix, which is `ConfigClient`. The prefix
 * helps in production to filter out logs from a particular component and this case, it helps to filter out logs generated
 * from config client.
 */
private[csw] object ConfigClientLogger extends LoggerFactory(Prefix(CSW, "ConfigClient"))
