package csw.config.client.commons

import csw.logging.client.scaladsl.LoggerFactory

/**
 * All the logs generated from config client will have a fixed componentName, which is `ConfigClient`. The componentName
 * helps in production to filter out logs from a particular component and this case, it helps to filter out logs generated
 * from config client.
 */
private[csw] object ConfigClientLogger extends LoggerFactory("ConfigClient")
