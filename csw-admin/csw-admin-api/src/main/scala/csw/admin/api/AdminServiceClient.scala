package csw.admin.api

import csw.admin.api.AdminServiceHttpMessage.{GetLogMetadata, SetLogMetadata}
import csw.location.models.ComponentId
import csw.logging.models.{Level, LogMetadata}
import msocket.api.Transport

import scala.concurrent.Future

class AdminServiceClient(postClient: Transport[AdminServiceHttpMessage]) extends AdminService with AdminServiceCodecs {
  def getLogMetadata(componentId: ComponentId): Future[LogMetadata] =
    postClient.requestResponse[LogMetadata](GetLogMetadata(componentId))
  def setLogLevel(componentId: ComponentId, level: Level): Future[Unit] =
    postClient.requestResponse[Unit](SetLogMetadata(componentId, level))
}
