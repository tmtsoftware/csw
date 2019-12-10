package csw.admin.server.commons

import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.CSW

private[admin] object AdminLogger extends LoggerFactory(Prefix(CSW, "admin_server"))
