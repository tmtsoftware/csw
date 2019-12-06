package csw.admin.api

import csw.location.models.ComponentId
import csw.logging.models.{Level, LogMetadata}

import scala.concurrent.Future

trait AdminService {
  def getLogMetadata(componentId: ComponentId): Future[LogMetadata]
  def setLogLevel(componentId: ComponentId, level: Level): Future[Unit]
}
