package csw.services.config.server.commons

import csw.services.logging.scaladsl.ServiceLogger

object ConfigServerLogger extends ServiceLogger(ConfigServiceConnection.value.name)
