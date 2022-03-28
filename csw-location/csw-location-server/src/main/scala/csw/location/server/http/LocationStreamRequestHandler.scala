/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.server.http

import csw.location.api.codec.LocationServiceCodecs._
import csw.location.api.messages.LocationStreamRequest
import csw.location.api.messages.LocationStreamRequest.Track
import csw.location.api.scaladsl.LocationService
import msocket.jvm.stream.{StreamRequestHandler, StreamResponse}

import scala.concurrent.Future

class LocationStreamRequestHandler(locationService: LocationService) extends StreamRequestHandler[LocationStreamRequest] {
  override def handle(request: LocationStreamRequest): Future[StreamResponse] =
    request match {
      case Track(connection) => stream(locationService.track(connection))
    }
}
