/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.server.http

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import csw.aas.http.AuthorizationPolicy.RealmRolePolicy
import csw.aas.http.SecurityDirectives
import csw.location.api.codec.LocationServiceCodecs._
import csw.location.api.messages.LocationRequest
import csw.location.api.messages.LocationRequest._
import csw.location.api.scaladsl.LocationService
import msocket.http.post.{HttpPostHandler, ServerHttpCodecs}

import scala.concurrent.ExecutionContext

class LocationRequestHandler(locationService: LocationService, securityDirectives: => SecurityDirectives)(implicit
    ex: ExecutionContext
) extends HttpPostHandler[LocationRequest]
    with ServerHttpCodecs {

  private lazy val securityDirectivesCached: SecurityDirectives = securityDirectives
  private val AdminRole                                         = "location-admin"

  override def handle(request: LocationRequest): Route =
    request match {
      case Register(registration)               => sPost(complete(locationService.register(registration).map(_.location)))
      case Unregister(connection)               => sPost(complete(locationService.unregister(connection)))
      case UnregisterAll                        => sPost(complete(locationService.unregisterAll()))
      case Find(connection)                     => complete(locationService.find(connection))
      case Resolve(connection, within)          => complete(locationService.resolve(connection, within))
      case ListEntries                          => complete(locationService.list)
      case ListByComponentType(componentType)   => complete(locationService.list(componentType))
      case ListByHostname(hostname)             => complete(locationService.list(hostname))
      case ListByConnectionType(connectionType) => complete(locationService.list(connectionType))
      case ListByPrefix(prefix)                 => complete(locationService.listByPrefix(prefix))
    }

  private def sPost(route: => Route): Route = securityDirectivesCached.sPost(RealmRolePolicy(AdminRole))(_ => route)
}
