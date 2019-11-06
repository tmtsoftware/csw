package csw.location.server.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.messages.LocationHttpMessage
import csw.location.api.messages.LocationHttpMessage._
import csw.location.api.scaladsl.LocationServiceE
import msocket.api.MessageHandler
import msocket.impl.post.ServerHttpCodecs

class LocationHttpHandler(locationServiceE: LocationServiceE)
    extends MessageHandler[LocationHttpMessage, Route]
    with LocationServiceCodecs
    with ServerHttpCodecs {

  override def handle(request: LocationHttpMessage): Route = request match {
    case Register(registration)               => complete(locationServiceE.register(registration))
    case Unregister(connection)               => complete(locationServiceE.unregister(connection))
    case UnregisterAll                        => complete(locationServiceE.unregisterAll())
    case Find(connection)                     => complete(locationServiceE.find(connection))
    case Resolve(connection, within)          => complete(locationServiceE.resolve(connection, within))
    case ListEntries                          => complete(locationServiceE.list)
    case ListByComponentType(componentType)   => complete(locationServiceE.list(componentType))
    case ListByHostname(hostname)             => complete(locationServiceE.list(hostname))
    case ListByConnectionType(connectionType) => complete(locationServiceE.list(connectionType))
    case ListByPrefix(prefix)                 => complete(locationServiceE.listByPrefix(prefix))
  }
}
