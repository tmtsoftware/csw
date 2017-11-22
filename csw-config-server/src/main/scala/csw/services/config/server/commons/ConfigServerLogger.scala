package csw.services.config.server.commons

import csw.services.logging.scaladsl.LibraryLogger

object ConfigServerLogger extends LibraryLogger(ConfigServiceConnection.value.componentId.name)
