package csw.services.config.server.commons

import csw.services.logging.scaladsl.CommonComponentLogger

object ConfigServerLogger extends CommonComponentLogger(ConfigServiceConnection.value.name)
