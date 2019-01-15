package csw.config.server.commons

import csw.logging.core.scaladsl.LoggerFactory

/**
 * All the logs generated from config service will have a fixed componentName, which is picked from `ConfigServiceConnection`.
 * The componentName helps in production to filter out logs from a particular component and this case, it helps to filter out logs
 * generated from config service.
 */
object ConfigServerLogger extends LoggerFactory(ConfigServiceConnection.value.componentId.name)
