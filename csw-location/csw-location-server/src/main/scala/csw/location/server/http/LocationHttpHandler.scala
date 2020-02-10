package csw.location.server.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import csw.aas.http.AuthorizationPolicy.EmptyPolicy
import csw.aas.http.SecurityDirectives
import csw.location.api.codec.LocationServiceCodecs._
import csw.location.api.messages.LocationHttpMessage
import csw.location.api.messages.LocationHttpMessage._
import csw.location.api.scaladsl.LocationService
import msocket.impl.post.{HttpPostHandler, ServerHttpCodecs}

import scala.concurrent.ExecutionContext

class LocationHttpHandler(locationService: LocationService, securityDirectives: SecurityDirectives)(
    implicit ex: ExecutionContext
) extends HttpPostHandler[LocationHttpMessage]
    with ServerHttpCodecs {

  import securityDirectives._

  override def handle(request: LocationHttpMessage): Route = request match {
    case Register(registration)               => secure(EmptyPolicy)(complete(locationService.register(registration).map(_.location)))
    case Unregister(connection)               => secure(EmptyPolicy)(complete(locationService.unregister(connection)))
    case UnregisterAll                        => secure(EmptyPolicy)(complete(locationService.unregisterAll()))
    case Find(connection)                     => complete(locationService.find(connection))
    case Resolve(connection, within)          => complete(locationService.resolve(connection, within))
    case ListEntries                          => complete(locationService.list)
    case ListByComponentType(componentType)   => complete(locationService.list(componentType))
    case ListByHostname(hostname)             => complete(locationService.list(hostname))
    case ListByConnectionType(connectionType) => complete(locationService.list(connectionType))
    case ListByPrefix(prefix)                 => complete(locationService.listByPrefix(prefix))
  }
}
