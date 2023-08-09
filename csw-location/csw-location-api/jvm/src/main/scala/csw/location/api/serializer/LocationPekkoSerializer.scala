/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.api.serializer

import csw.location.api.codec.{LocationCodecs, LocationSerializable}
import csw.location.api.models.{Connection, Location, Registration, TrackingEvent}
import csw.commons.CborPekkoSerializer

class LocationPekkoSerializer extends CborPekkoSerializer[LocationSerializable] with LocationCodecs {

  override val identifier: Int = 19924

  register[Connection]
  register[Location]
  register[Registration]
  register[TrackingEvent]
}
