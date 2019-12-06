package csw.admin.api

import csw.location.models.ComponentId
import csw.logging.models.Level

sealed trait AdminServiceHttpMessage

object AdminServiceHttpMessage {
  case class SetLogMetadata(componentId: ComponentId, level: Level) extends AdminServiceHttpMessage
  case class GetLogMetadata(componentId: ComponentId)               extends AdminServiceHttpMessage
}
