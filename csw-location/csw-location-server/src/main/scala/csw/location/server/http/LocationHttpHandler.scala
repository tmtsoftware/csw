package csw.location.server.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.messages.LocationHttpMessage
import csw.location.api.messages.LocationHttpMessage._
import csw.location.api.scaladsl.LocationService
import msocket.api.MessageHandler
import msocket.impl.post.ServerHttpCodecs

import scala.concurrent.ExecutionContext

class LocationHttpHandler(locationService: LocationService)(implicit ex: ExecutionContext)
    extends MessageHandler[LocationHttpMessage, Route]
    with LocationServiceCodecs
    with ServerHttpCodecs {

  override def handle(request: LocationHttpMessage): Route = request match {
    case Register(registration)               => complete(locationService.register(registration).map(_.location))
    case Unregister(connection)               => complete(locationService.unregister(connection))
    case UnregisterAll                        => complete(locationService.unregisterAll())
    case Find(connection)                     => complete(locationService.find(connection))
    case Resolve(connection, within)          => complete(locationService.resolve(connection, within))
    case ListEntries                          => complete(locationService.list)
    case ListByComponentType(componentType)   => complete(locationService.list(componentType))
    case ListByHostname(hostname)             => complete(locationService.list(hostname))
    case ListByConnectionType(connectionType) => complete(locationService.list(connectionType))
    case ListByPrefix(prefix)                 => complete(locationService.listByPrefix(prefix))
  }
}
