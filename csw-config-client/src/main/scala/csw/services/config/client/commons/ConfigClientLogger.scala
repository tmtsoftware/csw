package csw.services.config.client.commons

import csw.services.logging.scaladsl.ComponentLogger

object ConfigClientLogger extends ComponentLogger(s"${ConfigServiceConnection.value.componentId.fullName}-client")
