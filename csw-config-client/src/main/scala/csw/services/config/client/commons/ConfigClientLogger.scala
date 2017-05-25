package csw.services.config.client.commons

import csw.services.logging.scaladsl.ComponentLogger

object ConfigClientLogger extends ComponentLogger(s"${ConfigServiceConnection.componentId.fullName}-client")
