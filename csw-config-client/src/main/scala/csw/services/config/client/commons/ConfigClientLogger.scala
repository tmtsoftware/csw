package csw.services.config.client.commons

import csw.services.logging.scaladsl.CommonComponentLogger

object ConfigClientLogger extends CommonComponentLogger(s"${ConfigServiceConnection.value.componentId.fullName}-client")
