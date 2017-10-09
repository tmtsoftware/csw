package csw.services.config.client.commons

import csw.services.logging.scaladsl.ServiceLogger

object ConfigClientLogger extends ServiceLogger(s"${ConfigServiceConnection.value.componentId.fullName}-client")
