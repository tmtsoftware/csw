package csw.admin.handlers

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import csw.admin.api.{AdminServiceCodecs, AdminServiceHttpMessage}
import csw.admin.api.AdminServiceHttpMessage._
import csw.admin.impl.AdminServiceImpl
import msocket.api.MessageHandler
import msocket.impl.post.ServerHttpCodecs

class AdminHandlers(adminServiceImpl: AdminServiceImpl)
    extends MessageHandler[AdminServiceHttpMessage, Route]
    with AdminServiceCodecs
    with ServerHttpCodecs {
  override def handle(request: AdminServiceHttpMessage): Route = request match {
    case SetLogMetadata(componentId, logLevel) => complete(adminServiceImpl.setLogLevel(componentId, logLevel))
    case GetLogMetadata(componentId)           => complete(adminServiceImpl.getLogMetadata(componentId))
  }
}
