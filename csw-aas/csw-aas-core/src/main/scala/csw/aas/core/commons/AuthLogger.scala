package csw.aas.core.commons

import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.CSW

private[aas] object AuthLogger extends LoggerFactory(Prefix(CSW, "aas"))
